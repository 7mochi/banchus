package pe.nanamochi.banchus.service

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Beatmap
import pe.nanamochi.banchus.database.entity.Score
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.enums.BeatmapWebRankedStatus
import pe.nanamochi.banchus.domain.enums.LeaderboardType
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.enums.SubmissionStatus
import pe.nanamochi.banchus.redis.repository.LeaderboardRepository

private const val CURRENT_LEADERBOARD_VERSION = 4

@Service
class LeaderboardService(
    private val leaderboardRepository: LeaderboardRepository,
    @Lazy private val scoreService: ScoreService,
    private val beatmapService: BeatmapService,
    @Lazy private val userService: UserService,
) {
    fun addToLeaderboard(user: User, mode: Mode, performancePoints: Int) =
        leaderboardRepository.addToLeaderboard(user, mode, performancePoints)

    fun removeFromLeaderboard(user: User, mode: Mode) =
        leaderboardRepository.removeFromLeaderboard(user, mode)

    fun removeFromAllLeaderboards(user: User) {
        Mode.entries.forEach { mode -> removeFromLeaderboard(user, mode) }
    }

    fun fetchGlobalRank(userId: Int, mode: Mode) =
        leaderboardRepository.fetchGlobalRank(userId, mode)

    fun fetchBeatmapLeaderboard(
        user: User,
        beatmapMd5: String,
        leaderboardType: Int,
        leaderboardVersion: Int,
        modeInt: Int,
        modsBitmask: Int,
    ) = binding {
        if (leaderboardVersion != CURRENT_LEADERBOARD_VERSION) {
            userService
                .restrict(
                    user,
                    "The leaderboard version for the current known latest osu! client is $CURRENT_LEADERBOARD_VERSION, but the client sent $leaderboardVersion.",
                )
                .bind()
        }

        val beatmap = beatmapService.getOrCreateBeatmap(beatmapMd5).bind()
        val type = LeaderboardType.fromValue(leaderboardType)
        val mode = Mode.fromValue(modeInt)

        val modsToFilter = modsBitmask.takeIf { type == LeaderboardType.MODS }
        val country = user.country.takeIf { type == LeaderboardType.COUNTRY }

        val scores =
            scoreService.fetchLeaderboard(
                beatmap,
                mode,
                modsToFilter,
                SubmissionStatus.BEST,
                country,
            )
        val personalBest = scoreService.fetchBest(beatmap, user).get()
        formatLeaderboardResponse(scores, personalBest, user, beatmap)
    }

    private fun formatLeaderboardResponse(
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
