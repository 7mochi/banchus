package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Beatmap
import pe.nanamochi.banchus.database.entity.Score
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.database.repository.ScoreRepository
import pe.nanamochi.banchus.domain.enums.BeatmapRankedStatus
import pe.nanamochi.banchus.domain.enums.BeatmapWebRankedStatus
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.enums.SubmissionStatus
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.InternalError
import pe.nanamochi.banchus.domain.errors.ScoreNotFound
import pe.nanamochi.banchus.domain.errors.UserNotFound
import pe.nanamochi.banchus.dto.client.ParsedScore
import pe.nanamochi.banchus.util.Rijndael

@Service
class ScoreService(
    private val scoreRepository: ScoreRepository,
    private val chartService: ChartService,
    private val rankingService: RankingService,
    private val storageService: StorageService,
    private val statService: StatService,
    private val beatmapService: BeatmapService,
    private val performanceService: PerformanceService,
    private val presenceService: PresenceService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val beatmapLocks = ConcurrentHashMap<Int, ReentrantLock>()

    fun findById(id: Int): Result<Score, ScoreNotFound> {
        return scoreRepository.findScoreById(id).toResultOr { ScoreNotFound }
    }

    fun fetchLeaderboard(
        beatmap: Beatmap,
        mode: Mode?,
        mods: Int?,
        status: SubmissionStatus,
        country: CountryCode?,
    ): List<Score> {
        log.debug(
            "Fetching leaderboard for beatmap {} with mode {}, mods {}, status {}, country {}",
            beatmap.id,
            mode,
            mods,
            status,
            country,
        )

        return scoreRepository.fetchBeatmapLeaderboard(beatmap, mode, mods, status, country)
    }

    fun fetchBest(beatmap: Beatmap, user: User): Result<Score, ScoreNotFound> {
        log.debug("Fetching best score for user {} on beatmap {}", user.username, beatmap.id)

        return scoreRepository
            .findFirstByBeatmapAndUserAndSubmissionStatusOrderByPerformancePointsDesc(
                beatmap,
                user,
                SubmissionStatus.BEST,
            )
            .toResultOr { ScoreNotFound }
    }

    fun parseScore(
        request: HttpServletRequest,
        ivB64: String,
        osuVersion: String,
        scoreTime: Int,
    ): Result<ParsedScore, DomainMessage> = binding {
        val iv = runCatching { Base64.getDecoder().decode(ivB64) }.mapError { InternalError }.bind()

        val scoreParts = request.parts.filter { it.name == "score" }
        val scoreDataAesB64 =
            runCatching { scoreParts[0].inputStream.bufferedReader().use { it.readText() } }
                .mapError { InternalError }
                .bind()
        val replayBytes =
            runCatching { scoreParts[1].inputStream.use { it.readAllBytes() } }
                .mapError { InternalError }
                .bind()

        val aesKey = "osu!-scoreburgr---------$osuVersion".padEnd(32, ' ').take(32).toByteArray()
        val data =
            runCatching {
                    val encryptedBytes = Base64.getDecoder().decode(scoreDataAesB64)
                    val decryptedBytes = Rijndael.decrypt(encryptedBytes, aesKey, iv)
                    String(decryptedBytes).split(":")
                }
                .mapError { InternalError }
                .bind()

        val scoreEntity =
            Score().apply {
                onlineChecksum = data[2]
                score = data[9].toLong()
                highestCombo = data[10].toInt()
                fullCombo = data[11] == "1" || data[11].equals("True", ignoreCase = true)
                mods = data[13].toInt()
                num300s = data[3].toInt()
                num100s = data[4].toInt()
                num50s = data[5].toInt()
                numMisses = data[8].toInt()
                numGekis = data[6].toInt()
                numKatus = data[7].toInt()
                grade = data[12]
                mode = Mode.fromValue(data[15].toInt())
                passed = data[14] == "1" || data[14].equals("True", ignoreCase = true)
                timeElapsed = scoreTime
            }

        log.debug("Parsed score for user {} on beatmap {}", data[1].trim(), data[0])

        ParsedScore(
            score = scoreEntity,
            replayBytes = replayBytes,
            beatmapMd5 = data[0],
            username = data[1].trim(),
        )
    }

    @Transactional
    fun processScoreSubmission(
        parsedScore: ParsedScore,
        user: User,
        beatmap: Beatmap,
        session: Session,
    ): Result<String, DomainMessage> {
        val lock = beatmapLocks.computeIfAbsent(beatmap.id) { ReentrantLock() }

        return lock.withLock { executeScoreSubmission(parsedScore, user, beatmap, session) }
    }

    fun executeScoreSubmission(
        parsedScore: ParsedScore,
        user: User,
        beatmap: Beatmap,
        session: Session,
    ): Result<String, DomainMessage> = binding {
        val currentScore = parsedScore.score
        currentScore.user = user
        currentScore.beatmap = beatmap
        currentScore.updateAccuracy()

        // Download .osu file if not present or MD5 mismatch
        val beatmapData = beatmapService.getOrDownloadOsuFile(beatmap.id, beatmap.md5).bind()

        // Calculate performance points using external calculator
        val pp = performanceService.calculate(beatmapData, currentScore).bind()
        currentScore.performancePoints = pp

        // Find previous best score for this beatmap and user
        val previousBest =
            scoreRepository
                .findFirstByBeatmapAndUserAndSubmissionStatusOrderByPerformancePointsDesc(
                    beatmap,
                    user,
                    SubmissionStatus.BEST,
                )

        // Determine submission status
        if (!currentScore.passed) {
            currentScore.submissionStatus = SubmissionStatus.FAILED
        } else {
            val isNewBest =
                previousBest?.let { old -> currentScore.performancePoints > old.performancePoints }
                    ?: true

            if (isNewBest) {
                currentScore.submissionStatus = SubmissionStatus.BEST
                // Demote previous best score if exists
                previousBest?.apply {
                    submissionStatus = SubmissionStatus.SUBMITTED
                    scoreRepository.saveAndFlush(this)
                }
            } else {
                currentScore.submissionStatus = SubmissionStatus.SUBMITTED
            }
        }

        // Persist new score to database
        val savedScore = scoreRepository.saveAndFlush(currentScore)

        // Save replay file in storage
        storageService.saveReplay(savedScore.id.toLong(), parsedScore.replayBytes).bind()

        // Update beatmap stats (playcount, passcount)
        beatmapService.incrementStats(beatmap.id, currentScore.passed).bind()

        // Fetch and clone current mode stats
        val modeStats = statService.findByUserAndGamemode(user, savedScore.mode).bind()
        val previousModeStats = modeStats.clone()
        val previousGlobalRank = rankingService.getGlobalRank(savedScore.mode, user).toInt()

        // Update player stats with new score
        updatePlayerStats(modeStats, savedScore, previousBest).bind()
        val updatedModeStats = statService.update(modeStats).bind()
        rankingService.update(savedScore.mode, user, updatedModeStats)

        // Calculate new global rank
        val ownGlobalRank = rankingService.getGlobalRank(savedScore.mode, user).toInt()

        // Broadcast updated stats to all sessions
        presenceService.broadcastStats(session, user, updatedModeStats, ownGlobalRank).bind()

        // TODO: If this score is #1, send it to the #announce channel

        log.info("Processed score submission for user {} on beatmap {}.", user.username, beatmap.id)

        // Build beatmap ranking chart values for client
        chartService.buildCharts(
            beatmap,
            savedScore,
            previousBest,
            user,
            previousModeStats,
            updatedModeStats,
            previousGlobalRank,
            ownGlobalRank,
        )
    }

    private fun updatePlayerStats(
        stats: Stat,
        score: Score,
        previousBest: Score?,
    ): Result<Unit, DomainMessage> = binding {
        val user = stats.user.toResultOr { UserNotFound }.bind()

        val top100 =
            scoreRepository
                .findTop100ByUserAndModeAndSubmissionStatusInAndBeatmapStatusInOrderByPerformancePointsDesc(
                    user,
                    score.mode,
                    listOf(SubmissionStatus.BEST),
                    listOf(BeatmapRankedStatus.RANKED, BeatmapRankedStatus.APPROVED),
                )

        stats.accuracy = statService.calculateWeightedAccuracy(top100)
        stats.performancePoints = statService.calculateWeightedPp(top100)

        stats.totalScore += score.score
        stats.playCount += 1
        stats.playTime += score.timeElapsed

        val isRanked =
            score.beatmap?.status.let {
                it == BeatmapRankedStatus.RANKED || it == BeatmapRankedStatus.APPROVED
            }

        if (score.submissionStatus == SubmissionStatus.BEST && isRanked) {
            val previousScoreValue = previousBest?.score ?: 0L
            val increase = score.score - previousScoreValue
            stats.rankedScore += increase
        }
    }

    fun formatLeaderboardResponse(
        leaderboardScores: List<Score>,
        personalBest: Score?,
        user: User,
        beatmap: Beatmap,
    ): String =
        buildString {
                val status = BeatmapWebRankedStatus.convertToWebStatus(beatmap.status)

                append(
                    "$status|false|${beatmap.id}|${beatmap.beatmapset?.id}|${leaderboardScores.size}|0\n"
                )
                append(
                    "0\n${beatmap.beatmapset?.artist} - ${beatmap.beatmapset?.title} [${beatmap.version}]\n0.0\n"
                )

                append(personalBest.toLeaderboardLine(user, 1))

                leaderboardScores.forEachIndexed { index, score ->
                    append(score.toLeaderboardLine(score.user!!, index + 1))
                }
            }
            .trim()

    private fun Score?.toLeaderboardLine(user: User, rank: Int): String {
        return this?.run {
            val fc = if (fullCombo) 1 else 0
            "$id|${user.username}|$score|$highestCombo|$num50s|$num100s|$num300s|$numMisses|$numKatus|$numGekis|$fc|$mods|${user.id}|$rank|${createdAt.epochSecond}|1\n"
        } ?: "\n"
    }
}
