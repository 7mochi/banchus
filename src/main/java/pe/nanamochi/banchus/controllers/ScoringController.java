package pe.nanamochi.banchus.controllers;

import io.github.nanamochi.rosu_pp_jar.Mods;
import io.github.nanamochi.rosu_pp_jar.Performance;
import io.github.nanamochi.rosu_pp_jar.PerformanceAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.nanamochi.banchus.entities.Mode;
import pe.nanamochi.banchus.entities.SubmissionStatus;
import pe.nanamochi.banchus.entities.db.Beatmapset;
import pe.nanamochi.banchus.entities.db.Score;
import pe.nanamochi.banchus.entities.db.Session;
import pe.nanamochi.banchus.entities.db.User;
import pe.nanamochi.banchus.entities.osuapi.Beatmap;
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
  @Autowired private ReplayService replayService;
  @Autowired private OsuApi osuApi;

  @PostMapping(value = "/osu-submit-modular-selector.php")
  public String scoreSubmission(
      HttpServletRequest request,
      @RequestHeader("token") String token,
      @RequestParam("x") boolean exitedOut,
      @RequestParam("ft") int failTime,
      @RequestParam("iv") String ivB64,
      @RequestParam("st") int scoreTime,
      @RequestParam("pass") String passwordMd5,
      @RequestParam("osuver") String osuVersion,
      @RequestParam("s") String clientHashB64,
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
      // Malformed decrypted score data
    }

    String username = scoreData[1];

    User user = userService.login(username, passwordMd5);
    if (user == null) {}

    Session session = sessionService.getPrimarySessionByUsername(username);
    if (session == null) {}

    // TODO: handle differently depending on beatmap ranked status

    Score score = new Score();
    score.setOnlineChecksum(scoreData[2]);
    score.setBeatmapMd5(scoreData[0]);
    score.setScore(Integer.parseInt(scoreData[9]));
    score.setHighestCombo(Integer.parseInt(scoreData[10]));
    score.setFullCombo(Boolean.parseBoolean(scoreData[11]));
    score.setMods(Integer.parseInt(scoreData[13]));
    score.setNum300s(Integer.parseInt(scoreData[3]));
    score.setNum100s(Integer.parseInt(scoreData[4]));
    score.setNum50s(Integer.parseInt(scoreData[5]));
    score.setNumMisses(Integer.parseInt(scoreData[8]));
    score.setNumGekis(Integer.parseInt(scoreData[6]));
    score.setNumKatus(Integer.parseInt(scoreData[7]));
    score.setGrade(scoreData[12]);
    score.setSubmissionStatus(
        Boolean.parseBoolean(scoreData[14])
            ? SubmissionStatus.SUBMITTED
            : SubmissionStatus.FAILED); // TODO: determine best status
    score.setMode(Mode.fromValue(Integer.parseInt(scoreData[15])));
    score.setTimeElapsed(Boolean.parseBoolean(scoreData[14]) ? scoreTime : failTime);

    // Do a request to the osu!api to get beatmap info, because we need to get the beatmap_id from
    // the md5 hash, by default all beatmaps are returned independtly from the hash, so we need to
    // filter them here
    List<Beatmap> osuApibeatmaps = osuApi.getBeatmaps(score.getBeatmapMd5());
    Beatmap osuApibeatmap = null;
    for (Beatmap b : osuApibeatmaps) {
      if (b.getFileMd5().equalsIgnoreCase(score.getBeatmapMd5())) {
        osuApibeatmap = b;
        break;
      }
    }

    if (osuApibeatmap == null) {}

    // Check if the beatmapset exists in our database, if not, store the beatmapset and the beatmap
    Beatmapset beatmapset =
        beatmapsetService.findByBeatmapsetId(osuApibeatmaps.getFirst().getBeatmapsetId());
    if (beatmapset == null) {
      beatmapset = new Beatmapset();
      beatmapset.setId(osuApibeatmaps.getFirst().getBeatmapsetId());
      beatmapset.setTitle(osuApibeatmaps.getFirst().getTitle());
      beatmapset.setArtist(osuApibeatmaps.getFirst().getArtist());
      beatmapset.setSource(osuApibeatmaps.getFirst().getSource());
      beatmapset.setCreator(osuApibeatmaps.getFirst().getCreator());
      beatmapset.setDescription(
          null); // TODO: osu!api v1 does not provide description, save it when we change to v2
      beatmapset.setTags(osuApibeatmaps.getFirst().getTags());
      // 4 = loved, 3 = qualified, 2 = approved, 1 = ranked, 0 = pending,
      // -1 = WIP, -2 = graveyard
      beatmapset.setSubmissionStatus(osuApibeatmaps.getFirst().getApproved());
      beatmapset.setHasVideo(osuApibeatmaps.getFirst().isVideo());
      beatmapset.setHasStoryboard(osuApibeatmaps.getFirst().isStoryboard());
      beatmapset.setSubmissionDate(osuApibeatmaps.getFirst().getSubmitDate());
      beatmapset.setApprovedDate(
          osuApibeatmaps.getFirst().getApprovedDate() != null
              ? osuApibeatmaps.getFirst().getApprovedDate()
              : null);
      beatmapset.setLastUpdated(osuApibeatmaps.getFirst().getLastUpdate());
      beatmapset.setTotalPlaycount(0); // dont save bancho playcount, we will track our own
      // 0 = any, 1 = unspecified, 2 = english, 3 = japanese, 4 =
      // chinese, 5 = instrumental, 6 = korean, 7 = french, 8 = german, 9
      // = swedish, 10 = spanish, 11 = italian, 12 = russian, 13 =
      // polish, 14 = other
      beatmapset.setLanguageId(osuApibeatmaps.getFirst().getLanguageId());
      // 0 = any, 1 = unspecified, 2 = video game, 3 = anime, 4 = rock, 5 =
      // pop, 6 = other, 7 = novelty, 9 = hip hop, 10 = electronic, 11 =
      // metal, 12 = classical, 13 = folk, 14 = jazz (note that there's no
      // 8)
      beatmapset.setGenreId(osuApibeatmaps.getFirst().getGenreId());
      beatmapsetService.create(beatmapset);

      for (Beatmap b : osuApibeatmaps) {
        pe.nanamochi.banchus.entities.db.Beatmap beatmap =
            new pe.nanamochi.banchus.entities.db.Beatmap();
        beatmap.setId(b.getBeatmapId());
        beatmap.setMode(Mode.fromValue(b.getMode()));
        beatmap.setMd5(b.getFileMd5());
        beatmap.setStatus(b.getApproved());
        beatmap.setVersion(b.getVersion());
        beatmap.setSubmissionDate(b.getSubmitDate());
        beatmap.setLastUpdated(b.getLastUpdate());
        beatmap.setPlaycount(0); // dont save bancho playcount, we will track our own
        beatmap.setPasscount(0); // dont save bancho passcount, we will track our own
        beatmap.setTotalLength(b.getTotalLength());
        beatmap.setDrainLength(b.getHitLength());
        beatmap.setCountNormal(b.getCountNormal());
        beatmap.setCountSlider(b.getCountSlider());
        beatmap.setCountSpinner(b.getCountSpinner());
        beatmap.setMaxCombo(b.getMaxCombo());
        beatmap.setBpm(b.getBpm());
        beatmap.setCs(b.getDiffSize());
        beatmap.setAr(b.getDiffApproach());
        beatmap.setOd(b.getDiffOverall());
        beatmap.setHp(b.getDiffDrain());
        beatmap.setStarRating(b.getDifficultyRating());

        beatmapService.create(beatmap);
        beatmapService.getOrDownloadOsuFile(b.getBeatmapId(), b.getFileMd5());
      }
    }

    // Fetch the beatmap saved in our filesystem, if not found, download it and save it
    // TODO: use beatmap.getId instead of osuApibeatmap.getBeatmapId()
    byte[] osuFile =
        beatmapService.getOrDownloadOsuFile(osuApibeatmap.getBeatmapId(), score.getBeatmapMd5());

    // Calculate pp (move this to a method like calculateAccuracy)
    io.github.nanamochi.rosu_pp_jar.Beatmap rosuBeatmap =
        io.github.nanamochi.rosu_pp_jar.Beatmap.fromBytes(osuFile);
    // rosuBeatmap.convert(GameMode.fromValues) // TODO: implement fromValue in rosu_pp_jar
    Performance performance = Performance.create(rosuBeatmap);
    performance.setMods(Mods.fromBits(score.getMods()));
    performance.setAccuracy(score.getAccuracy());
    performance.setNGeki(score.getNumGekis());
    performance.setNGeki(score.getNumKatus());
    performance.setN300(score.getNum300s());
    performance.setN100(score.getNum100s());
    performance.setN50(score.getNum50s());
    performance.setMisses(score.getNumMisses());
    performance.setCombo(score.getHighestCombo());
    PerformanceAttributes attributes = performance.calculate();

    double accuracy = calculateAccuracy(score);
    score.setPerformancePoints(attributes.pp());
    score.setAccuracy(accuracy);

    // Persist new score to database
    score = scoreService.saveScore(score);

    // Save replay in our filesystem
    replayService.saveReplay(score.getId(), replayBytes);

    // Update beatmap stats (plays, passes)
    pe.nanamochi.banchus.entities.db.Beatmap beatmap =
        beatmapService.findByMd5(score.getBeatmapMd5());
    beatmap.setPlaycount(beatmap.getPlaycount() + 1);
    beatmap.setPasscount(
        score.getSubmissionStatus() != SubmissionStatus.FAILED
            ? beatmap.getPasscount() + 1
            : beatmap.getPasscount());
    beatmapService.update(beatmap);

    // TODO: update account statsd

    // TODO: calculate new overall accuracy

    // TODO: calculate new overall pp

    // TODO: update this gamemode's stats with our new score submission

    // TODO: send account stats to all other osu! sessions if we're not restricted

    // TODO: if this score is #1, send it to the #announce channel

    // TODO: unlock achievements

    // TODO: build beatmap ranking chart values

    // TODO: build overall ranking chart values

    // TODO: construct response data

    // TODO: add overall and beatmap ranking charts to response data

    // TODO: add newly unlocked achievements to response data

    // Print all variables to test
    logger.info("Score submission received:");
    logger.info("Username: " + username);
    logger.info("Score: " + score);

    // Score: Score(id=1, user=null, onlineChecksum=3a1419d73b8497e64fdf7882c9b26ca3,
    // beatmapMd5=971fc1aa53ab6d3691d765406d328672, score=43644, performancePoints=0,
    // accuracy=86.29629629629629, highestCombo=51, fullCombo=false, mods=576, num300s=36,
    // num100s=8, num50s=1, numMisses=0, numGekis=9, numKatus=4, grade=A,
    // submissionStatus=SUBMITTED, mode=OSU, timeElapsed=24736,
    // createdAt=2026-01-29T18:15:01.235108954Z, updatedAt=2026-01-29T18:15:01.235108954Z)       :

    /*Data[0]: 971fc1aa53ab6d3691d765406d328672
    Data[1]: test
    Data[2]: 684e0165b10abe5fe5fa699efe3b4745
    Data[3]: 35
    Data[4]: 8
    Data[5]: 1
    Data[6]: 8
    Data[7]: 5
    Data[8]: 1
    Data[9]: 54899
    Data[10]: 61
    Data[11]: False
    Data[12]: C
    Data[13]: 576
    Data[14]: True
    Data[15]: 0
    Data[16]: 260129163524
    Data[17]: 20260116
    Data[18]: 47136405
         */

    byte[] clientHash = Base64.getDecoder().decode(clientHashB64);

    return "Score submission endpoint - to be implemented";
  }

  private double calculateAccuracy(Score score) {
    if (score.getMode() == Mode.OSU) {
      int totalNotes =
          score.getNum300s() + score.getNum100s() + score.getNum50s() + score.getNumMisses();
      return (100.0
          * ((score.getNum300s() * 300.0)
              + (score.getNum100s() * 100.0)
              + (score.getNum50s() * 50.0))
          / (totalNotes * 300.0));
    } else if (score.getMode() == Mode.TAIKO) {
      int totalNotes = score.getNum300s() + score.getNum100s() + score.getNumMisses();
      return (100.0 * ((score.getNum100s() * 0.5) + score.getNum300s()) / totalNotes);
    } else if (score.getMode() == Mode.CATCH) {
      int totalNotes =
          score.getNum300s()
              + score.getNum100s()
              + score.getNum50s()
              + score.getNumKatus()
              + score.getNumMisses();
      return (100.0 * (score.getNum300s() + score.getNum100s() + score.getNum50s())) / totalNotes;
    } else if (score.getMode() == Mode.MANIA) {
      int totalNotes =
          score.getNum300s()
              + score.getNum100s()
              + score.getNum50s()
              + score.getNumGekis()
              + score.getNumKatus()
              + score.getNumMisses();
      return (100.0
          * ((score.getNum50s() * 50.0)
              + (score.getNum100s() * 100.0)
              + (score.getNumKatus() * 200.0)
              + ((score.getNum300s() + score.getNumGekis()) * 300.0))
          / (totalNotes * 300.0));
    } else {
      return 0.0;
    }
  }
}
