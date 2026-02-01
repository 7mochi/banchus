package pe.nanamochi.banchus.services;

import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.entities.db.Beatmap;
import pe.nanamochi.banchus.entities.db.Score;
import pe.nanamochi.banchus.entities.db.Stat;
import pe.nanamochi.banchus.entities.db.User;

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
      beatmapRankBefore = "0"; // TODO
      beatmapRankedScoreBefore = String.valueOf(previousBestScore.getScore());
      beatmapTotalScoreBefore = String.valueOf(previousBestScore.getScore());
      beatmapMaxComboBefore = String.valueOf(previousBestScore.getHighestCombo());
      beatmapAccuracyBefore = String.format("%.2f", previousBestScore.getAccuracy());
      beatmapPerformancePointsBefore =
          String.valueOf(Math.round(previousBestScore.getPerformancePoints()));
    }

    String beatmapRankAfter = "1";
    String beatmapRankedScoreAfter = String.valueOf(score.getScore());
    String beatmapTotalScoreAfter = String.valueOf(score.getScore());
    String beatmapMaxComboAfter = String.valueOf(score.getHighestCombo());
    String beatmapAccuracyAfter = String.format("%.2f", score.getAccuracy());
    String beatmapPerformancePointsAfter = String.valueOf(Math.round(score.getPerformancePoints()));

    return "beatmapId:"
        + beatmap.getId()
        + "|"
        + "beatmapSetId:"
        + beatmap.getBeatmapset().getId()
        + "|"
        + "beatmapPlaycount:"
        + beatmap.getPlaycount()
        + "|"
        + "beatmapPasscount:"
        + beatmap.getPasscount()
        + "|"
        + "approvedDate:"
        + beatmap.getSubmissionDate()
        + "|"
        + "\n|chartId:beatmap|"
        + "chartUrl:https://osu.ppy.sh/beatmapsets/"
        + beatmap.getBeatmapset().getId()
        + "|"
        + "chartName:Beatmap Ranking|"
        + "rankBefore:"
        + beatmapRankBefore
        + "|"
        + "rankAfter:"
        + beatmapRankAfter
        + "|"
        + "rankedScoreBefore:"
        + beatmapRankedScoreBefore
        + "|"
        + "rankedScoreAfter:"
        + beatmapRankedScoreAfter
        + "|"
        + "totalScoreBefore:"
        + beatmapTotalScoreBefore
        + "|"
        + "totalScoreAfter:"
        + beatmapTotalScoreAfter
        + "|"
        + "maxComboBefore:"
        + beatmapMaxComboBefore
        + "|"
        + "maxComboAfter:"
        + beatmapMaxComboAfter
        + "|"
        + "accuracyBefore:"
        + beatmapAccuracyBefore
        + "|"
        + "accuracyAfter:"
        + beatmapAccuracyAfter
        + "|"
        + "ppBefore:"
        + beatmapPerformancePointsBefore
        + "|"
        + "ppAfter:"
        + beatmapPerformancePointsAfter
        + "|"
        + "onlineScoreId:"
        + score.getId()
        + "|"
        + "\n|chartId:overall|"
        + "chartUrl:https://osu.ppy.sh/u/"
        + user.getId()
        + "|"
        + "chartName:Overall Ranking|"
        + "rankBefore:"
        + previousGlobalRank
        + "|"
        + "rankAfter:"
        + ownGlobalRank
        + "|"
        + "rankedScoreBefore:"
        + previousModeStats.getRankedScore()
        + "|"
        + "rankedScoreAfter:"
        + modeStats.getRankedScore()
        + "|"
        + "totalScoreBefore:"
        + previousModeStats.getTotalScore()
        + "|"
        + "totalScoreAfter:"
        + modeStats.getTotalScore()
        + "|"
        + "maxComboBefore:"
        + previousModeStats.getHighestCombo()
        + "|"
        + "maxComboAfter:"
        + modeStats.getHighestCombo()
        + "|"
        + "accuracyBefore:"
        + String.format("%.2f", previousModeStats.getAccuracy())
        + "|"
        + "accuracyAfter:"
        + String.format("%.2f", modeStats.getAccuracy())
        + "|"
        + "ppBefore:"
        + previousModeStats.getPerformancePoints()
        + "|"
        + "ppAfter:"
        + modeStats.getPerformancePoints()
        + "|";
  }
}
