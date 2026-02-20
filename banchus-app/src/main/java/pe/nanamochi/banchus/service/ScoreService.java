package pe.nanamochi.banchus.service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nanamochi.banchus.database.entity.*;
import pe.nanamochi.banchus.database.repository.ScoreRepository;
import pe.nanamochi.banchus.domain.dto.ParsedScore;
import pe.nanamochi.banchus.domain.enums.*;
import pe.nanamochi.banchus.packets.server.UserStatsPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.PacketBundle;

@Service
@RequiredArgsConstructor
public class ScoreService {
  private final ScoreRepository scoreRepository;
  private final SessionService sessionService;
  private final ChartService chartService;
  private final RankingService rankingService;
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  // private final ChannelService channelService;
  private final StorageService storageService;
  private final StatService statService;
  private final BeatmapService beatmapService;
  private final PerformanceService performanceService;

  public List<Score> fetchBeatmapLeaderboard(
      Beatmap beatmap, Mode mode, Integer mods, SubmissionStatus status, CountryCode country) {

    if (mods != null) {
      if (country != null) {
        return scoreRepository.findTop50UnrestrictedWithModsByCountry(
            beatmap, mode, mods, status, country);
      }
      return scoreRepository.findTop50UnrestrictedWithMods(beatmap, mode, mods, status);
    }

    if (country != null) {
      return scoreRepository.findTop50UnrestrictedByCountry(beatmap, mode, status, country);
    }

    return scoreRepository.findTop50Unrestricted(beatmap, mode, status);
  }

  public Optional<Score> findById(Integer id) {
    return scoreRepository.findById(id);
  }

  public Optional<Score> findBest(Beatmap beatmap, User user) {
    return scoreRepository.findFirstByBeatmapAndUserAndSubmissionStatusOrderByPerformancePointsDesc(
        beatmap, user, SubmissionStatus.BEST);
  }

  public String formatLeaderboardResponse(
      List<Score> leaderboardScores, Score personalBest, User user, Beatmap beatmap) {

    StringBuilder sb = new StringBuilder();

    sb.append(BeatmapWebRankedStatus.convertToWebStatus(beatmap.getStatus()))
        .append("|")
        .append("false")
        .append("|")
        .append(beatmap.getId())
        .append("|")
        .append(beatmap.getBeatmapset().getId())
        .append("|")
        .append(leaderboardScores.size())
        .append("|")
        .append("0")
        .append("|\n");

    String beatmapFullTitle =
        String.format(
            "%s - %s [%s]",
            beatmap.getBeatmapset().getArtist(),
            beatmap.getBeatmapset().getTitle(),
            beatmap.getVersion());

    sb.append("0\n").append(beatmapFullTitle).append("\n").append("0.0\n"); // TODO: rating

    // If user has no personal best, send empty line
    if (personalBest != null) {
      sb.append(formatScoreLine(personalBest, user, 1)); // TODO: leaderboard rank, TODO: has replay
    } else {
      sb.append("\n");
    }

    int rank = 1;
    for (Score s : leaderboardScores) {
      sb.append(formatScoreLine(s, s.getUser(), rank++));
    }

    return sb.toString().trim();
  }

  private String formatScoreLine(Score s, User u, int rank) {
    return String.format(
        "%d|%s|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%1d\n",
        s.getId(),
        u.getUsername(),
        s.getScore(),
        s.getHighestCombo(),
        s.getNum50s(),
        s.getNum100s(),
        s.getNum300s(),
        s.getNumMisses(),
        s.getNumKatus(),
        s.getNumGekis(),
        s.isFullCombo() ? 1 : 0,
        s.getMods(),
        u.getId(),
        rank,
        s.getCreatedAt().getEpochSecond(),
        1 // TODO: has replay
        );
  }

  @Transactional
  public String processScoreSubmission(
      ParsedScore parsedScore, User user, Beatmap beatmap, Session session) throws Exception {
    final Score currentScore = parsedScore.score();
    currentScore.setUser(user);
    currentScore.setBeatmap(beatmap);

    // Download .osu file if not present or MD5 mismatch
    beatmapService.getOrDownloadOsuFile(beatmap.getId(), beatmap.getMd5());
    String beatmapPath = beatmapService.getBeatmapPath(beatmap.getId()).toAbsolutePath().toString();

    // Calculate performance points using external calculator
    double pp = performanceService.calculate(beatmapPath, currentScore);
    currentScore.setPerformancePoints(pp);

    // Find previous best score for this beatmap and user
    Optional<Score> previousBest =
        scoreRepository.findFirstByBeatmapAndUserAndSubmissionStatusOrderByPerformancePointsDesc(
            beatmap, user, SubmissionStatus.BEST);

    // Determine submission status
    if (!currentScore.isPassed()) {
      currentScore.setSubmissionStatus(SubmissionStatus.FAILED);
    } else {
      boolean isNewBest =
          previousBest
              .map(old -> currentScore.getPerformancePoints() > old.getPerformancePoints())
              .orElse(true);

      if (isNewBest) {
        currentScore.setSubmissionStatus(SubmissionStatus.BEST);
        // Demote previous best score if exists
        previousBest.ifPresent(
            old -> {
              old.setSubmissionStatus(SubmissionStatus.SUBMITTED);
              scoreRepository.save(old);
            });
      } else {
        currentScore.setSubmissionStatus(SubmissionStatus.SUBMITTED);
      }
    }

    // Persist new score to database
    Score savedScore = scoreRepository.save(currentScore);

    // Save replay file in storage
    storageService.saveReplay(savedScore.getId(), parsedScore.replayBytes());

    // Update beatmap stats (playcount, passcount)
    beatmap.setPlaycount(beatmap.getPlaycount() + 1);
    if (savedScore.isPassed()) beatmap.setPasscount(beatmap.getPasscount() + 1);
    beatmapService.update(beatmap);

    // Fetch and clone current mode stats
    Stat modeStats =
        statService
            .findByUserAndGamemode(user, savedScore.getMode())
            .orElseThrow(
                () ->
                    new IllegalStateException("Stats not found for mode: " + savedScore.getMode()));

    Stat previousModeStats = (Stat) modeStats.clone();
    int previousGlobalRank =
        Math.toIntExact(rankingService.getGlobalRank(savedScore.getMode(), user));

    // Update player stats with new score
    updatePlayerStats(modeStats, savedScore, previousBest.orElse(null));
    modeStats = statService.update(modeStats);
    rankingService.update(savedScore.getMode(), user, modeStats);

    // Calculate new global rank
    int ownGlobalRank = Math.toIntExact(rankingService.getGlobalRank(savedScore.getMode(), user));

    // Broadcast updated stats to all sessions
    broadcastStats(session, user, modeStats, ownGlobalRank);

    // TODO: If this score is #1, send it to the #announce channel

    // Build beatmap ranking chart values for client
    return chartService.buildCharts(
        beatmap,
        savedScore,
        previousBest.orElse(null),
        user,
        previousModeStats,
        modeStats,
        previousGlobalRank,
        ownGlobalRank);
  }

  private void updatePlayerStats(Stat stats, Score score, Score previousBest) {
    // Fetch top 100 best scores for weighted calculations
    List<Score> top100 =
        scoreRepository
            .findTop100ByUserAndModeAndSubmissionStatusInAndBeatmapStatusInOrderByPerformancePointsDesc(
                stats.getUser(),
                score.getMode(),
                List.of(SubmissionStatus.BEST),
                List.of(BeatmapRankedStatus.RANKED, BeatmapRankedStatus.APPROVED));

    // Calculate weighted accuracy and pp
    stats.setAccuracy(statService.calculateWeightedAccuracy(top100));
    stats.setPerformancePoints((int) statService.calculateWeightedPp(top100));

    stats.setTotalScore(stats.getTotalScore() + score.getScore());
    stats.setPlayCount(stats.getPlayCount() + 1);
    stats.setPlayTime(stats.getPlayTime() + score.getTimeElapsed());

    // Update ranked score only if new best and beatmap is ranked
    if (score.getSubmissionStatus() == SubmissionStatus.BEST
        && beatmapIsRanked(score.getBeatmap())) {
      long increase = score.getScore() - (previousBest != null ? previousBest.getScore() : 0);
      stats.setRankedScore(stats.getRankedScore() + increase);
    }
  }

  private boolean beatmapIsRanked(Beatmap b) {
    return b.getStatus() == BeatmapRankedStatus.RANKED
        || b.getStatus() == BeatmapRankedStatus.APPROVED;
  }

  private void broadcastStats(Session session, User user, Stat stats, int rank) throws Exception {
    UserStatsPacket packet =
        new UserStatsPacket(
            user.getId(),
            session.getAction(),
            session.getInfoText(),
            session.getBeatmapMd5(),
            session.getMods(),
            stats.getGamemode(),
            session.getBeatmapId(),
            stats.getRankedScore(),
            (float) stats.getAccuracy(),
            stats.getPlayCount(),
            stats.getTotalScore(),
            rank,
            stats.getPerformancePoints());

    List<Session> targets = user.isRestricted() ? List.of(session) : sessionService.findAll();

    for (Session target : targets) {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      packetWriter.writePacket(os, packet);
      packetBundleService.enqueue(target.getId(), new PacketBundle(os.toByteArray()));
    }
  }
}
