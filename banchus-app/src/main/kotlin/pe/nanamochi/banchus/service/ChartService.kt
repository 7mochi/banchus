package pe.nanamochi.banchus.service

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Beatmap
import pe.nanamochi.banchus.database.entity.Score
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.database.entity.User

@Service
class ChartService {
    fun buildSubmissionCharts(
        beatmap: Beatmap,
        score: Score,
        user: User,
        previousBest: Score?,
        previousBestRank: Int?,
        newBeatmapRank: Int,
        oldGlobalRank: UInt,
        newGlobalRank: UInt,
        oldStats: Stat,
        stats: Stat,
    ): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val approvedDate =
            LocalDateTime.ofInstant(beatmap.lastUpdated, ZoneOffset.UTC).format(formatter)

        val beatmapRankingChart =
            listOf(
                chartEntry("rank", previousBestRank, newBeatmapRank),
                chartEntry("rankedScore", previousBest?.score, score.score),
                chartEntry("totalScore", previousBest?.score, score.score),
                chartEntry("maxCombo", previousBest?.highestCombo, score.highestCombo),
                chartEntry(
                    "accuracy",
                    previousBest?.accuracy?.let { "%.2f".format(it) },
                    "%.2f".format(score.accuracy),
                ),
                chartEntry(
                    "pp",
                    previousBest?.performancePoints?.roundToInt(),
                    score.performancePoints.roundToInt(),
                ),
            )

        val overallRankingChart =
            listOf(
                chartEntry("rank", oldGlobalRank, newGlobalRank),
                chartEntry("rankedScore", oldStats.rankedScore, stats.rankedScore),
                chartEntry("totalScore", oldStats.totalScore, stats.totalScore),
                chartEntry("maxCombo", oldStats.maxCombo, stats.maxCombo),
                chartEntry(
                    "accuracy",
                    "%.2f".format(oldStats.averageAccuracy),
                    "%.2f".format(stats.averageAccuracy),
                ),
                chartEntry("pp", oldStats.performancePoints, stats.performancePoints),
            )

        val achievementsStr = "" // TODO: Implement unlock achievements

        val submissionCharts =
            mutableListOf(
                "beatmapId:${beatmap.id}",
                "beatmapSetId:${beatmap.beatmapset!!.id}",
                "beatmapPlaycount:${beatmap.playcount}",
                "beatmapPasscount:${beatmap.passcount}",
                "approvedDate:$approvedDate",
                "\n",
                "chartId:beatmap",
                "chartUrl:https://osu.ppy.sh/b/${beatmap.id}", // TODO: change this with my url
                "chartName:Beatmap Ranking",
            )

        submissionCharts.addAll(beatmapRankingChart)
        submissionCharts.add("onlineScoreId:${score.id}")
        submissionCharts.add("\n")
        submissionCharts.add("chartId:overall")
        submissionCharts.add(
            "chartUrl:https://osu.ppy.sh/u/${user.id}"
        ) // TODO: change this with my url
        submissionCharts.add("chartName:Overall Ranking")
        submissionCharts.addAll(overallRankingChart)
        submissionCharts.add("achievements-new:$achievementsStr")

        return submissionCharts.joinToString("|")
    }

    private fun <T> chartEntry(name: String, before: T?, after: T): String =
        "${name}Before:${before ?: ""}|${name}After:$after"
}
