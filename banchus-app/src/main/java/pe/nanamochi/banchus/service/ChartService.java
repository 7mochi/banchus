package pe.nanamochi.banchus.service;

import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.database.entity.Beatmap;
import pe.nanamochi.banchus.database.entity.Score;
import pe.nanamochi.banchus.database.entity.Stat;
import pe.nanamochi.banchus.database.entity.User;

@Service
public class ChartService {
  public String buildCharts(
      Beatmap beatmap,
      Score score,
      Score previousBestScore,
      User user,
      Stat previousModeStats,
      Stat modeStats,
      int previousGlobalRank,
      int ownGlobalRank) {
    String beatmapRankBefore = "";
    String beatmapRankedScoreBefore = "";
    String beatmapTotalScoreBefore = "";
    String beatmapMaxComboBefore = "";
    String beatmapAccuracyBefore = "";
    String beatmapPerformancePointsBefore = "";

    if (previousBestScore != null) {
      beatmapRankBefore = "0"; // TODO: Implement leaderboard position logic
      beatmapRankedScoreBefore = String.valueOf(previousBestScore.getScore());
      beatmapTotalScoreBefore = String.valueOf(previousBestScore.getScore());
      beatmapMaxComboBefore = String.valueOf(previousBestScore.getHighestCombo());
      beatmapAccuracyBefore = String.format("%.2f", previousBestScore.getAccuracy());
      beatmapPerformancePointsBefore =
          String.valueOf(Math.round(previousBestScore.getPerformancePoints()));
    }

    StringBuilder sb = new StringBuilder();

    sb.append("beatmapId:")
        .append(beatmap.getId())
        .append("|beatmapSetId:")
        .append(beatmap.getBeatmapset().getId())
        .append("|beatmapPlaycount:")
        .append(beatmap.getPlaycount())
        .append("|beatmapPasscount:")
        .append(beatmap.getPasscount())
        .append("|approvedDate:")
        .append(beatmap.getSubmissionDate())
        .append("|\n");

    sb.append("|chartId:beatmap")
        .append("|chartUrl:https://osu.ppy.sh/beatmapsets/")
        .append(beatmap.getBeatmapset().getId())
        .append("|chartName:Beatmap Ranking")
        .append("|rankBefore:")
        .append(beatmapRankBefore)
        .append("|rankAfter:1") // TODO: Implement leaderboard position logic
        .append("|rankedScoreBefore:")
        .append(beatmapRankedScoreBefore)
        .append("|rankedScoreAfter:")
        .append(score.getScore())
        .append("|totalScoreBefore:")
        .append(beatmapTotalScoreBefore)
        .append("|totalScoreAfter:")
        .append(score.getScore())
        .append("|maxComboBefore:")
        .append(beatmapMaxComboBefore)
        .append("|maxComboAfter:")
        .append(score.getHighestCombo())
        .append("|accuracyBefore:")
        .append(beatmapAccuracyBefore)
        .append("|accuracyAfter:")
        .append(String.format("%.2f", score.getAccuracy()))
        .append("|ppBefore:")
        .append(beatmapPerformancePointsBefore)
        .append("|ppAfter:")
        .append(Math.round(score.getPerformancePoints()))
        .append("|onlineScoreId:")
        .append(score.getId())
        .append("|\n");

    sb.append("|chartId:overall")
        .append("|chartUrl:https://osu.ppy.sh/u/")
        .append(user.getId())
        .append("|chartName:Overall Ranking")
        .append("|rankBefore:")
        .append(previousGlobalRank)
        .append("|rankAfter:")
        .append(ownGlobalRank)
        .append("|rankedScoreBefore:")
        .append(previousModeStats.getRankedScore())
        .append("|rankedScoreAfter:")
        .append(modeStats.getRankedScore())
        .append("|totalScoreBefore:")
        .append(previousModeStats.getTotalScore())
        .append("|totalScoreAfter:")
        .append(modeStats.getTotalScore())
        .append("|maxComboBefore:")
        .append(previousModeStats.getHighestCombo())
        .append("|maxComboAfter:")
        .append(modeStats.getHighestCombo())
        .append("|accuracyBefore:")
        .append(String.format("%.2f", previousModeStats.getAccuracy()))
        .append("|accuracyAfter:")
        .append(String.format("%.2f", modeStats.getAccuracy()))
        .append("|ppBefore:")
        .append(previousModeStats.getPerformancePoints())
        .append("|ppAfter:")
        .append(modeStats.getPerformancePoints())
        .append("|");

    return sb.toString();
  }
}
