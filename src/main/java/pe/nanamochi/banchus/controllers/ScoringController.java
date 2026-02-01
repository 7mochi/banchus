package pe.nanamochi.banchus.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.nanamochi.banchus.entities.*;
import pe.nanamochi.banchus.entities.db.*;
import pe.nanamochi.banchus.entities.osuapi.Beatmap;
import pe.nanamochi.banchus.mappers.BeatmapMapper;
import pe.nanamochi.banchus.mappers.BeatmapsetMapper;
import pe.nanamochi.banchus.packets.PacketWriter;
import pe.nanamochi.banchus.packets.server.MessagePacket;
import pe.nanamochi.banchus.packets.server.UserStatsPacket;
import pe.nanamochi.banchus.services.*;
import pe.nanamochi.banchus.utils.OsuApi;
import pe.nanamochi.banchus.utils.Rijndael;

@RestController
@RequestMapping("/web")
public class ScoringController {
  private static final Logger logger = LoggerFactory.getLogger(ScoringController.class);

  @Autowired private UserService userService;
  @Autowired private SessionService sessionService;
  @Autowired private ScoreService scoreService;
  @Autowired private BeatmapsetService beatmapsetService;
  @Autowired private BeatmapService beatmapService;
  @Autowired private BeatmapMapper beatmapMapper;
  @Autowired private BeatmapsetMapper beatmapsetMapper;
  @Autowired private ReplayService replayService;
  @Autowired private StatService statService;
  @Autowired private OsuApi osuApi;
  @Autowired private PacketWriter packetWriter;
  @Autowired private PacketBundleService packetBundleService;
  @Autowired private ChannelService channelService;
  @Autowired private ChannelMembersRedisService channelMembersRedisService;
  @Autowired private RankingService rankingService;
  @Autowired private UserStatCalculationService userStatCalculationService;
  @Autowired private ChartService chartService;

  @PostMapping(
      value = "/osu-submit-modular-selector.php",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public String scoreSubmission(
      HttpServletRequest request,
      @RequestParam(value = "ft", required = false) Integer failTime,
      @RequestParam(value = "iv", required = false) String ivB64,
      @RequestParam(value = "st", required = false) Integer scoreTime,
      @RequestParam(value = "pass", required = false) String passwordMd5,
      @RequestParam(value = "osuver", required = false) String osuVersion,
      @RequestParam(value = "s", required = false) String clientHashB64,
      @RequestPart(value = "i", required = false) MultipartFile flCheatScreenshot)
      throws Exception {
    byte[] iv = Base64.getDecoder().decode(ivB64);

    // The bancho protocol uses the "score" parameter name for both the base64'ed score data,
    // and the replay file in the multipart. @RequestPart can´t handle it well, so we manually
    // handle it here with HttpServletRequest
    List<Part> scoreParts =
        request.getParts().stream().filter(p -> p.getName().equals("score")).toList();
    Part scoreDataPart = scoreParts.get(0);
    Part replayPart = scoreParts.get(1);

    String scoreDataAesB64 =
        new String(scoreDataPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    byte[] encryptedScoreData = Base64.getDecoder().decode(scoreDataAesB64);
    byte[] replayBytes = replayPart.getInputStream().readAllBytes();

    // Ensure AES key is exactly 32 bytes
    String keyStr = ("osu!-scoreburgr---------" + osuVersion);
    keyStr = String.format("%-32s", keyStr).substring(0, 32);
    byte[] aesKey = keyStr.getBytes(StandardCharsets.UTF_8);

    // Decrypt score data
    byte[] decryptedBytes = Rijndael.decrypt(encryptedScoreData, aesKey, iv);
    String decrypted = new String(decryptedBytes, StandardCharsets.UTF_8);
    String[] scoreData = decrypted.split(":");

    if (scoreData.length < 13) {
      logger.info("A submitted score has a malformed score data.");
      return "error: " + ScoreSubmissionErrors.NO.getValue();
    }

    String username = scoreData[1].stripTrailing();

    User user = userService.login(username, passwordMd5);
    if (user == null) {
      return "error: " + ScoreSubmissionErrors.NEEDS_AUTHENTICATION.getValue();
    }

    Session session = sessionService.getPrimarySessionByUsername(username);
    if (session == null) {
      return "error: " + ScoreSubmissionErrors.NEEDS_AUTHENTICATION.getValue();
    }

    // TODO: handle differently depending on beatmap ranked status

    boolean isPassed = Boolean.parseBoolean(scoreData[14]);
    String beatmapMd5 = scoreData[0];
    String onlineChecksum = scoreData[2];
    int num300s = Integer.parseInt(scoreData[3]);
    int num100s = Integer.parseInt(scoreData[4]);
    int num50s = Integer.parseInt(scoreData[5]);
    int numGekis = Integer.parseInt(scoreData[6]);
    int numKatus = Integer.parseInt(scoreData[7]);
    int numMisses = Integer.parseInt(scoreData[8]);
    int scorePoints = Integer.parseInt(scoreData[9]);
    int highestCombo = Integer.parseInt(scoreData[10]);
    boolean fullCombo = Boolean.parseBoolean(scoreData[11]);
    String grade = scoreData[12];
    int mods = Integer.parseInt(scoreData[13]);
    int mode = Integer.parseInt(scoreData[15]);

    // Do a request to the osu!api to get beatmap info, because we need to get the beatmap_id from
    // the md5 hash, by default all beatmaps are returned independtly from the hash, so we need to
    // filter them here
    Beatmap osuApibeatmap = osuApi.getBeatmap(beatmapMd5);

    if (osuApibeatmap == null) {
      return "error: " + ScoreSubmissionErrors.BEATMAP_UNRANKED.getValue();
    }

    // Check if the beatmapset exists in our database, if not, store the beatmapset and the beatmap
    Beatmapset beatmapset = beatmapsetService.findByBeatmapsetId(osuApibeatmap.getBeatmapsetId());
    if (beatmapset == null) {
      beatmapset = beatmapsetMapper.fromApi(osuApibeatmap);
      beatmapsetService.create(beatmapset);

      List<Beatmap> osuApibeatmaps = osuApi.getBeatmaps(osuApibeatmap.getBeatmapsetId());
      for (Beatmap b : osuApibeatmaps) {
        var beatmap = beatmapMapper.fromApi(b);
        beatmap.setBeatmapset(beatmapset);

        beatmapService.create(beatmap);
        beatmapService.getOrDownloadOsuFile(b.getBeatmapId(), b.getFileMd5());
      }
    }

    Score score = new Score();
    score.setUser(user);
    score.setOnlineChecksum(onlineChecksum);
    score.setBeatmap(beatmapService.findByMd5(beatmapMd5));
    score.setScore(scorePoints);
    score.setHighestCombo(highestCombo);
    score.setFullCombo(fullCombo);
    score.setMods(mods);
    score.setNum300s(num300s);
    score.setNum100s(num100s);
    score.setNum50s(num50s);
    score.setNumMisses(numMisses);
    score.setNumGekis(numGekis);
    score.setNumKatus(numKatus);
    score.setGrade(grade);
    score.setMode(Mode.fromValue(mode));
    score.setTimeElapsed(isPassed ? scoreTime : failTime);

    double pp =
        scoreService.calculatePp(
            beatmapService.getOrDownloadOsuFile(osuApibeatmap.getBeatmapId(), beatmapMd5), score);

    var beatmap = beatmapService.findByMd5(beatmapMd5);
    SubmissionStatus submissionStatus;
    Score previousBestScore = null;

    if (isPassed) {
      previousBestScore = scoreService.getBestScore(beatmap, user);
      boolean isNewBest =
          previousBestScore == null || pp > previousBestScore.getPerformancePoints();

      if (isNewBest) {
        submissionStatus = SubmissionStatus.BEST;
        if (previousBestScore != null) {
          previousBestScore.setSubmissionStatus(SubmissionStatus.SUBMITTED);
          scoreService.updateScore(previousBestScore);
        }
      } else {
        submissionStatus = SubmissionStatus.SUBMITTED;
      }
    } else {
      submissionStatus = SubmissionStatus.FAILED;
    }

    score.setSubmissionStatus(submissionStatus);
    score.setPerformancePoints(pp);

    // Persist new score to database
    score = scoreService.saveScore(score);

    // Save replay in our filesystem
    replayService.saveReplay(score.getId(), replayBytes);

    // Update beatmap stats (plays, passes)
    beatmap.setPlaycount(beatmap.getPlaycount() + 1);
    beatmap.setPasscount(
        score.getSubmissionStatus() != SubmissionStatus.FAILED
            ? beatmap.getPasscount() + 1
            : beatmap.getPasscount());
    beatmapService.update(beatmap);

    Stat modeStats = statService.getStats(user, score.getMode());
    List<Score> top100Scores = scoreService.getUserTop100(user, score.getMode());
    int totalScoreCount = scoreService.getUserBestScoresCount(user, score.getMode());

    // Calculate new overall accuracy
    float weightedAccuracy = userStatCalculationService.calculateWeightedAccuracy(top100Scores);
    float bonusAccuracy = 0.0f;
    if (totalScoreCount > 0) {
      bonusAccuracy = (float) (100.0f / (20 * (1 - Math.pow(0.95f, totalScoreCount))));
    }
    float totalAccuracy = (weightedAccuracy * bonusAccuracy) / 100.0f;

    // Calculate new overall pp
    float weightedPp = userStatCalculationService.calculateWeightedPp(top100Scores);
    float bonusPp = (float) (416.6667f * (1 - Math.pow(0.9994f, totalScoreCount)));
    float totalPp = Math.round(weightedPp + bonusPp);

    // Create a copy of the previous gamemode's stats.
    // We will use this to construct overall ranking charts for the client
    Stat previousModeStats = (Stat) modeStats.clone();
    int previousGlobalRank =
        Math.toIntExact(rankingService.getGlobalRank(Mode.fromValue(mode), user));
    long newRankedScore = modeStats.getRankedScore();

    if (score.getSubmissionStatus() == SubmissionStatus.BEST
        && (beatmap.getStatus() == BeatmapRankedStatus.RANKED
            || beatmap.getStatus() == BeatmapRankedStatus.APPROVED)) {
      newRankedScore += scorePoints;

      if (previousBestScore != null) {
        newRankedScore -= previousBestScore.getScore();
      }
    }

    // Update this gamemode's stats with our new score submission
    modeStats.setGamemode(score.getMode());
    modeStats.setTotalScore(modeStats.getTotalScore() + scorePoints);
    modeStats.setRankedScore(newRankedScore);
    modeStats.setPerformancePoints((int) totalPp);
    modeStats.setPlayCount(modeStats.getPlayCount() + 1);
    modeStats.setPlayTime(modeStats.getPlayTime() + score.getTimeElapsed());
    modeStats.setAccuracy(totalAccuracy);
    modeStats.setHighestCombo(Math.max(modeStats.getHighestCombo(), score.getHighestCombo()));
    modeStats.setTotalHits(
        modeStats.getTotalHits()
            + score.getNum300s()
            + score.getNum100s()
            + score.getNum50s()
            + score.getNumMisses());
    modeStats.setXhCount(modeStats.getXhCount() + (score.getGrade().equals("XH") ? 1 : 0));
    modeStats.setXCount(modeStats.getXCount() + (score.getGrade().equals("X") ? 1 : 0));
    modeStats.setShCount(modeStats.getShCount() + (score.getGrade().equals("SH") ? 1 : 0));
    modeStats.setSCount(modeStats.getSCount() + (score.getGrade().equals("S") ? 1 : 0));
    modeStats.setACount(modeStats.getACount() + (score.getGrade().equals("A") ? 1 : 0));
    statService.update(modeStats);

    rankingService.updateRanking(Mode.fromValue(mode), user, modeStats);

    // Send account stats to all other osu! sessions if we're not restricted
    List<Session> osuSessionsToNotify;
    if (user.isRestricted()) {
      osuSessionsToNotify = List.of(session);
    } else {
      osuSessionsToNotify = sessionService.getAllSessions();
    }

    int ownGlobalRank = Math.toIntExact(rankingService.getGlobalRank(Mode.fromValue(mode), user));

    for (Session otherOsuSession : osuSessionsToNotify) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      packetWriter.writePacket(
          stream,
          new UserStatsPacket(
              modeStats.getUser().getId(),
              session.getAction(),
              session.getInfoText(),
              session.getBeatmapMd5(),
              session.getMods(),
              Mode.fromValue(mode),
              session.getBeatmapId(),
              modeStats.getRankedScore(),
              modeStats.getAccuracy(),
              modeStats.getPlayCount(),
              modeStats.getTotalScore(),
              ownGlobalRank,
              modeStats.getPerformancePoints()));
      packetBundleService.enqueue(otherOsuSession.getId(), new PacketBundle(stream.toByteArray()));
    }

    // TODO: calculate score rank on the beatmap
    int scoreRank = 1;

    // If this score is #1, send it to the #announce channel
    if (score.getSubmissionStatus() == SubmissionStatus.BEST && scoreRank == 1) {
      Channel announceChannel = channelService.findByName("#announce");
      if (announceChannel != null) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        packetWriter.writePacket(
            stream,
            new MessagePacket("BanchoBot", beatmap.createBeatmapChatEmbed(), "#announce", 0));
        Set<UUID> announceChannelMembers =
            channelMembersRedisService.getMembers(announceChannel.getId());
        for (UUID osuSessionId : announceChannelMembers) {
          packetBundleService.enqueue(osuSessionId, new PacketBundle(stream.toByteArray()));
        }
      }
    }

    // TODO: unlock achievements

    // Build beatmap ranking chart values

    logger.info(
        "[{}] {} submitted a score | ({}), {}pp",
        score.getMode().getAlias(),
        score.getUser().getUsername(),
        score.getSubmissionStatus(),
        String.format("%.2f", score.getPerformancePoints()));

    return chartService.buildCharts(
        beatmap,
        score,
        previousBestScore,
        user,
        previousModeStats,
        modeStats,
        previousGlobalRank,
        ownGlobalRank);
  }
}
