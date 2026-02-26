package pe.nanamochi.banchus.service

import kotlin.math.roundToLong
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Beatmap
import pe.nanamochi.banchus.database.entity.Score
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.database.entity.User

@Service
class ChartService {
    fun buildCharts(
        beatmap: Beatmap,
        score: Score,
        previousBestScore: Score?,
        user: User,
        previousModeStats: Stat,
        modeStats: Stat,
        previousGlobalRank: Int,
        ownGlobalRank: Int,
    ): String {
        val beatmapSetId = beatmap.beatmapset?.id ?: 0

        fun Double.formatAcc() = "%.2f".format(this)
        fun Double.toLongStr() = this.roundToLong().toString()

        val rankedScoreBefore = previousBestScore?.score ?: 0L
        val maxComboBefore = previousBestScore?.highestCombo ?: 0
        val accuracyBefore = previousBestScore?.accuracy?.formatAcc() ?: "0.00"
        val ppBefore = previousBestScore?.performancePoints?.toLongStr() ?: "0"

        val overallAccBefore = previousModeStats.accuracy.formatAcc()
        val overallAccAfter = modeStats.accuracy.formatAcc()

        val sb = StringBuilder()

        sb.append("beatmapId:${beatmap.id}|")
        sb.append("beatmapSetId:$beatmapSetId|")
        sb.append("beatmapPlaycount:${beatmap.playcount}|")
        sb.append("beatmapPasscount:${beatmap.passcount}|")
        sb.append("approvedDate:${beatmap.submissionDate}|")

        sb.append("\nchartId:beatmap|")
        sb.append("chartUrl:https://osu.ppy.sh/beatmapsets/$beatmapSetId|")
        sb.append("chartName:Beatmap Ranking|")
        sb.append("rankBefore:${previousBestScore?.let { "0" } ?: ""}|") // TODO: Leaderboard pos
        sb.append("rankAfter:1|")
        sb.append("rankedScoreBefore:$rankedScoreBefore|")
        sb.append("rankedScoreAfter:${score.score}|")
        sb.append("totalScoreBefore:$rankedScoreBefore|")
        sb.append("totalScoreAfter:${score.score}|")
        sb.append("maxComboBefore:$maxComboBefore|")
        sb.append("maxComboAfter:${score.highestCombo}|")
        sb.append("accuracyBefore:$accuracyBefore|")
        sb.append("accuracyAfter:${score.accuracy.formatAcc()}|")
        sb.append("ppBefore:$ppBefore|")
        sb.append("ppAfter:${score.performancePoints.toLongStr()}|")
        sb.append("onlineScoreId:${score.id}|")

        sb.append("\nchartId:overall|")
        sb.append("chartUrl:https://osu.ppy.sh/u/${user.id}|")
        sb.append("chartName:Overall Ranking|")
        sb.append("rankBefore:$previousGlobalRank|")
        sb.append("rankAfter:$ownGlobalRank|")
        sb.append("rankedScoreBefore:${previousModeStats.rankedScore}|")
        sb.append("rankedScoreAfter:${modeStats.rankedScore}|")
        sb.append("totalScoreBefore:${previousModeStats.totalScore}|")
        sb.append("totalScoreAfter:${modeStats.totalScore}|")
        sb.append("maxComboBefore:${previousModeStats.highestCombo}|")
        sb.append("maxComboAfter:${modeStats.highestCombo}|")
        sb.append("accuracyBefore:$overallAccBefore|")
        sb.append("accuracyAfter:$overallAccAfter|")
        sb.append("ppBefore:${previousModeStats.performancePoints}|")
        sb.append("ppAfter:${modeStats.performancePoints}")

        return sb.toString()
    }
}
