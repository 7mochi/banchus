package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import jakarta.servlet.http.HttpServletRequest
import java.time.Instant
import java.util.Base64
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.nanamochi.banchus.components.hasAny
import pe.nanamochi.banchus.database.entity.Beatmap
import pe.nanamochi.banchus.database.entity.Score
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.database.repository.ScoreRepository
import pe.nanamochi.banchus.domain.enums.BeatmapRankedStatus
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.enums.Mods
import pe.nanamochi.banchus.domain.enums.SubmissionStatus
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.domain.error.InternalError
import pe.nanamochi.banchus.domain.error.ScoreNotFound
import pe.nanamochi.banchus.domain.error.SessionNotFound
import pe.nanamochi.banchus.dto.client.DecryptedScoreData
import pe.nanamochi.banchus.infrastructure.redis.RedisDistributedLock
import pe.nanamochi.banchus.packets.server.MessagePacket
import pe.nanamochi.banchus.packets.server.UserStatsPacket
import pe.nanamochi.banchus.redis.stream.StreamName
import pe.nanamochi.banchus.util.Rijndael
import pe.nanamochi.banchus.util.toBanchoUser

@Service
class ScoreService(
    private val scoreRepository: ScoreRepository,
    private val beatmapService: BeatmapService,
    private val storageService: StorageService,
    private val statService: StatService,
    private val sessionService: SessionService,
    private val userService: UserService,
    private val performanceService: PerformanceService,
    private val leaderboardService: LeaderboardService,
    private val presenceService: PresenceService,
    private val streamService: StreamService,
    private val lock: RedisDistributedLock,
    private val chartService: ChartService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun fetchOneById(id: Long) = scoreRepository.findScoreById(id).toResultOr { ScoreNotFound }

    fun fetchOneByOnlineChecksum(onlineChecksum: String) =
        scoreRepository.findScoreByOnlineChecksum(onlineChecksum).toResultOr { ScoreNotFound }

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
            .findFirstByBeatmapAndUserIdAndSubmissionStatusOrderByPerformancePointsDesc(
                beatmap,
                user.id,
                SubmissionStatus.BEST,
            )
            .toResultOr { ScoreNotFound }
    }

    fun parseForm(request: HttpServletRequest): Result<Pair<String, ByteArray>, DomainMessage> =
        binding {
            val scoreParts = request.parts.filter { it.name == "score" }
            val scoreDataAesB64 =
                runCatching { scoreParts[0].inputStream.bufferedReader().use { it.readText() } }
                    .mapError { InternalError }
                    .bind()
            val replayBytes =
                runCatching { scoreParts[1].inputStream.use { it.readAllBytes() } }
                    .mapError { InternalError }
                    .bind()

            Pair(scoreDataAesB64, replayBytes)
        }

    fun decryptScore(
        scoreDataAesB64: String,
        clientHashB64: String,
        ivB64: String,
        osuVersion: String,
    ): Result<Pair<List<String>, ByteArray>, InternalError> = binding {
        val iv = runCatching { Base64.getDecoder().decode(ivB64) }.mapError { InternalError }.bind()
        val aesKey = "osu!-scoreburgr---------$osuVersion".padEnd(32, ' ').take(32).toByteArray()

        val scoreData =
            runCatching {
                    val encryptedBytes = Base64.getDecoder().decode(scoreDataAesB64)
                    val decryptedBytes = Rijndael.decrypt(encryptedBytes, aesKey, iv)
                    String(decryptedBytes).split(":")
                }
                .mapError { InternalError }
                .bind()
        val clientHashDecoded =
            runCatching {
                    val encryptedBytes = Base64.getDecoder().decode(clientHashB64)
                    Rijndael.decrypt(encryptedBytes, aesKey, iv)
                }
                .mapError { InternalError }
                .bind()

        Pair(scoreData, clientHashDecoded)
    }

    @Transactional
    fun submitScore(
        request: HttpServletRequest,
        headers: HttpHeaders,
        ivB64: String,
        clientHashB64: String,
        scoreTime: Int,
        passwordMd5: String,
        osuVersion: String,
    ) = binding {
        val (scoreDataB64, replayFile) = parseForm(request).bind()
        val (scoreTokens, _) = decryptScore(scoreDataB64, clientHashB64, ivB64, osuVersion).bind()
        val decrypted = DecryptedScoreData.fromTokens(scoreTokens).bind()

        val user = userService.login(decrypted.username, passwordMd5).bind()
        val sessions = sessionService.fetchByUsername(decrypted.username)
        if (sessions.isEmpty()) return@binding Err(SessionNotFound).bind()

        val beatmap = beatmapService.getOrCreateBeatmap(decrypted.beatmapMd5).bind()
        val score =
            Score(
                user = user,
                onlineChecksum = decrypted.onlineChecksum,
                beatmap = beatmap,
                score = decrypted.score.toLong(),
                highestCombo = decrypted.highestCombo,
                fullCombo = decrypted.fullCombo,
                mods = decrypted.mods,
                num300s = decrypted.n300,
                num100s = decrypted.n100,
                num50s = decrypted.n50,
                numMisses = decrypted.nMiss,
                numGekis = decrypted.nGeki,
                numKatus = decrypted.nKatu,
                grade = decrypted.grade,
                mode = Mode.fromValue(decrypted.mode),
                passed = decrypted.passed,
            )

        score.accuracy = score.calculateAccuracy()

        val previousBest =
            scoreRepository
                .findFirstByBeatmapAndUserIdAndSubmissionStatusOrderByPerformancePointsDesc(
                    beatmap,
                    user.id,
                    SubmissionStatus.BEST,
                )

        user.latestActivity = Instant.now()
        userService.update(user)

        if (!areModsRankableForBeatmap(score.mods.toUInt(), beatmap)) {
            log.warn(
                "Score submission denied for ${user.username} (${user.id} in beatmap ${beatmap.id}) due to unrankable mods (${Mods.fromBitmask(score.mods.toUInt())})."
            )
            return@binding Err(InternalError).bind() // TODO: maybe change to a more specific error
        }

        headers.getFirst("user-agent")?.let { userAgent ->
            if (userAgent != "osu!")
                userService
                    .restrict(
                        user,
                        "The expected user-agent header for an osu! client is 'osu!', while the client sent $userAgent.",
                    )
                    .bind()
        }

        if (Mods.hasConflict(score.mods.toUInt())) {
            userService
                .restrict(
                    user,
                    "The user attempted to submit a score with the mod combination ${Mods.fromBitmask(score.mods.toUInt())}, which contains mutually exclusive/illegal mods.",
                )
                .bind()
        }

        val lockKey = "score_submission:${score.onlineChecksum}"
        if (lock.acquireLock(lockKey, 15000, TimeUnit.MILLISECONDS)) {
            val scoreExists = fetchOneByOnlineChecksum(score.onlineChecksum).isOk
            if (scoreExists)
                return@binding Err(InternalError).bind() // TODO: maybe a more specific error

            score.performancePoints =
                performanceService.calculate(beatmap.id, beatmap.md5, score).bind()
            score.submissionStatus = calculateStatus(score, previousBest)
            score.timeElapsed = scoreTime

            scoreRepository.save(score)

            lock.releaseLock(lockKey)
        }

        // TODO: Update most played (Table not implemented yet)

        if (score.passed) {
            if (replayFile.size < 24) {
                userService
                    .restrict(
                        user,
                        "The user attempted to submit a completed score without a replay attached. This should NEVER happen and means they are likely using a replay editor.",
                    )
                    .bind()
            } else {
                storageService.saveReplay(score.id, replayFile)
            }
        }

        val stats = statService.fetchOne(user.id, score.mode).bind()
        val oldStats = stats.clone()

        var totalHits = score.num300s + score.num100s
        if (score.mode != Mode.CATCH) totalHits += score.num50s
        if (score.mode == Mode.TAIKO || score.mode == Mode.MANIA)
            totalHits += score.numGekis + score.numKatus

        stats.playCount += 1
        stats.playTime += score.timeElapsed / 1000
        stats.totalScore += score.score
        stats.totalHits += totalHits

        if (score.passed && beatmap.hasLeaderboard()) {
            if (stats.maxCombo < score.highestCombo) stats.maxCombo = score.highestCombo

            if (score.performancePoints > 0.0) {
                val top100 =
                    scoreRepository
                        .findTop100ByUserAndModeAndSubmissionStatusInAndBeatmapStatusInOrderByPerformancePointsDesc(
                            user,
                            score.mode,
                            listOf(SubmissionStatus.BEST),
                            listOf(BeatmapRankedStatus.RANKED, BeatmapRankedStatus.APPROVED),
                        )

                val rankedScoreCount =
                    scoreRepository
                        .countRankedScores(
                            user.id,
                            score.mode,
                            SubmissionStatus.BEST,
                            listOf(BeatmapRankedStatus.RANKED, BeatmapRankedStatus.APPROVED),
                        )
                        .toInt()

                stats.averageAccuracy = statService.calculateWeightedAccuracy(top100)
                stats.performancePoints = statService.calculateWeightedPp(top100, rankedScoreCount)
            }

            if (
                (beatmap.status == BeatmapRankedStatus.RANKED ||
                    beatmap.status == BeatmapRankedStatus.APPROVED) &&
                    score.submissionStatus == SubmissionStatus.BEST
            ) {
                val grade = score.calculateGrade()
                stats.adjustGradeCounter(grade, +1)
                stats.rankedScore += score.score

                previousBest?.let {
                    stats.rankedScore -= it.score
                    val previousBestGrade = previousBest.calculateGrade()
                    stats.adjustGradeCounter(previousBestGrade, -1)
                }
            }
        }

        // TODO: increment playcount

        val oldGlobalRank = leaderboardService.fetchGlobalRank(user.id, score.mode)

        if (
            score.submissionStatus == SubmissionStatus.BEST &&
                !user.isRestricted &&
                oldStats.performancePoints != stats.performancePoints
        ) {
            statService.update(stats.apply { latestPerformancePointAwarded = Instant.now() })
            leaderboardService.addToLeaderboard(user, stats.mode, stats.performancePoints)
        }

        val newGlobalRank = leaderboardService.fetchGlobalRank(user.id, score.mode)

        val newBeatmapRank =
            if (score.passed && beatmap.hasLeaderboard()) {
                scoreRepository
                    .findBeatmapRank(
                        beatmap.id,
                        score.mode,
                        SubmissionStatus.BEST,
                        user.id,
                        score.score,
                        score.id,
                    )
                    .toInt()
            } else {
                0
            }

        val previousBestRank =
            previousBest?.let {
                scoreRepository
                    .findBeatmapRank(
                        beatmap.id,
                        score.mode,
                        SubmissionStatus.BEST,
                        user.id,
                        it.score,
                        it.id,
                    )
                    .toInt()
            }

        if (
            newBeatmapRank == 1 &&
                score.submissionStatus == SubmissionStatus.BEST &&
                beatmap.hasLeaderboard() &&
                !user.isRestricted
        ) {
            scoreRepository.upsertFirstPlace(
                beatmap.id,
                score.mode.value.toInt(),
                score.id,
                user.id,
            )

            streamService.broadcastMessage(
                StreamName.Channel("#announce"),
                MessagePacket(
                    sender = "BanchoBot",
                    content =
                        "${user.username} has achieved #1 on ${beatmap.beatmapset?.artist} - ${beatmap.beatmapset?.title} [${beatmap.version}] +${Mods.fromBitmask(score.mods.toUInt())} (${"%.2f".format(score.performancePoints)}pp)",
                    target = "#announce",
                    senderId = 1,
                ),
            )
        }

        refreshStats(user, stats)

        // TODO: Handle multiplayer

        chartService.buildSubmissionCharts(
            beatmap = beatmap,
            score = score,
            user = user,
            previousBest = previousBest,
            previousBestRank = previousBestRank,
            newBeatmapRank = newBeatmapRank,
            oldGlobalRank = oldGlobalRank,
            newGlobalRank = newGlobalRank,
            oldStats = oldStats,
            stats = stats,
        )
    }

    fun calculateStatus(score: Score, previousBest: Score?): SubmissionStatus {
        if (!score.passed) return SubmissionStatus.FAILED
        val best = previousBest ?: return SubmissionStatus.BEST

        // Spin to win: same pp, higher raw score still counts as new best
        val isNewBest =
            score.performancePoints > best.performancePoints ||
                (score.performancePoints == best.performancePoints && score.score > best.score)
        return if (isNewBest) SubmissionStatus.BEST else SubmissionStatus.SUBMITTED
    }

    fun areModsRankableForBeatmap(mods: UInt, beatmap: Beatmap): Boolean {
        if (mods.hasAny(Mods.AUTOPLAY.value)) return false

        if (beatmap.mode == Mode.OSU || beatmap.mode == Mode.CATCH) {
            if (beatmap.objectCount() >= 7000) {
                if (!mods.hasAny(Mods.SCORE_V2.value)) return false
            } else {
                if (mods.hasAny(Mods.SCORE_V2.value)) return false
            }
        } else {
            if (mods.hasAny(Mods.SCORE_V2.value)) return false
        }

        return true
    }

    fun refreshStats(user: User, stats: Stat) {
        presenceService.fetchOne(user.id)?.let { initialPresence ->
            val globalRank = leaderboardService.fetchGlobalRank(user.id, stats.mode)
            initialPresence.globalRank = globalRank
            val updatedPresence = presenceService.update(initialPresence)

            val statsPacket = UserStatsPacket(updatedPresence.toBanchoUser())
            if (!user.isRestricted) {
                streamService.broadcastMessage(StreamName.Main, statsPacket)
            } else {
                sessionService.fetchByUserId(user.id).forEach { session ->
                    streamService.broadcastMessage(StreamName.User(session.sessionId), statsPacket)
                }
            }
        }

        log.info("Successfully handled update stats event for user ${user.username} (${user.id})")
    }
}
