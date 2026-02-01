package pe.nanamochi.banchus.services;

import io.github.nanamochi.rosu_pp_jar.Mods;
import io.github.nanamochi.rosu_pp_jar.Performance;
import io.github.nanamochi.rosu_pp_jar.PerformanceAttributes;
import io.github.nanamochi.rosu_pp_jar.RosuException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.entities.BeatmapRankedStatus;
import pe.nanamochi.banchus.entities.CountryCode;
import pe.nanamochi.banchus.entities.Mode;
import pe.nanamochi.banchus.entities.SubmissionStatus;
import pe.nanamochi.banchus.entities.db.Beatmap;
import pe.nanamochi.banchus.entities.db.Score;
import pe.nanamochi.banchus.entities.db.User;
import pe.nanamochi.banchus.repositories.ScoreRepository;
import pe.nanamochi.banchus.utils.Rijndael;

@Service
public class ScoreService {
  @Autowired private ScoreRepository scoreRepository;

  public Score getScoreById(Integer id) {
    return scoreRepository.findById(id).orElse(null);
  }

  public Score saveScore(Score score) {
    return scoreRepository.save(score);
  }

  public Score updateScore(Score score) {
    if (!scoreRepository.existsById(score.getId())) {
      throw new IllegalArgumentException("Score not found: " + score.getId());
    }
    return scoreRepository.save(score);
  }

  public List<Score> getUserTop100(User user, Mode mode) {
    return scoreRepository
        .findTop100ByUserAndModeAndSubmissionStatusInAndBeatmapStatusInOrderByPerformancePointsDesc(
            user,
            mode,
            List.of(SubmissionStatus.BEST),
            List.of(BeatmapRankedStatus.RANKED, BeatmapRankedStatus.APPROVED));
  }

  public int getUserBestScoresCount(User user, Mode mode) {
    return scoreRepository.countByUserAndModeAndSubmissionStatusInAndBeatmapStatusIn(
        user,
        mode,
        List.of(SubmissionStatus.BEST),
        List.of(BeatmapRankedStatus.RANKED, BeatmapRankedStatus.APPROVED));
  }

  public Score getBestScore(Beatmap beatmap, User user) {
    return scoreRepository.findFirstByBeatmapAndUserAndSubmissionStatusOrderByPerformancePointsDesc(
        beatmap, user, SubmissionStatus.BEST);
  }

  public List<Score> getBeatmapLeaderboard(
      Beatmap beatmap, Mode mode, Integer mods, SubmissionStatus status, CountryCode country) {
    if (mods != null) {
      if (country != null) {
        return scoreRepository
            .findTop50ByBeatmapAndModeAndModsAndSubmissionStatusAndUser_RestrictedFalseAndUser_CountryOrderByScoreDesc(
                beatmap, mode, mods, status, country);
      }

      return scoreRepository
          .findTop50ByBeatmapAndModeAndModsAndSubmissionStatusAndUser_RestrictedFalseOrderByScoreDesc(
              beatmap, mode, mods, status);
    }

    if (country != null) {
      return scoreRepository
          .findTop50ByBeatmapAndModeAndSubmissionStatusAndUser_RestrictedFalseAndUser_CountryOrderByScoreDesc(
              beatmap, mode, status, country);
    }

    return scoreRepository
        .findTop50ByBeatmapAndModeAndSubmissionStatusAndUser_RestrictedFalseOrderByScoreDesc(
            beatmap, mode, status);
  }

  public ParsedScoreDTO parseScoreData(
      HttpServletRequest request,
      String ivB64,
      String osuVersion,
      Integer scoreTime,
      Integer failTime)
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

    User user = new User();
    user.setUsername(scoreData[1].stripTrailing());

    Beatmap beatmap = new Beatmap();
    beatmap.setMd5(scoreData[0]);

    Score score = new Score();
    score.setUser(user); // Temporary user, used to login later
    score.setOnlineChecksum(scoreData[2]);
    score.setBeatmap(beatmap); // Temporary beatmap, used to check later
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
    score.setMode(Mode.fromValue(Integer.parseInt(scoreData[15])));
    score.setPassed(Boolean.parseBoolean(scoreData[14]));
    score.setTimeElapsed(Boolean.parseBoolean(scoreData[14]) ? scoreTime : failTime);

    return new ParsedScoreDTO(score, replayBytes);
  }

  public double calculatePp(byte[] osuFile, Score score) throws RosuException {
    io.github.nanamochi.rosu_pp_jar.Beatmap rosuBeatmap =
        io.github.nanamochi.rosu_pp_jar.Beatmap.fromBytes(osuFile);
    // rosuBeatmap.convert(GameMode.fromValues) // TODO: implement fromValue in rosu_pp_jar
    Performance performance = Performance.create(rosuBeatmap);
    performance.setMods(Mods.fromBits(score.getMods()));
    performance.setAccuracy((double) score.getAccuracy());
    performance.setNGeki(score.getNumGekis());
    performance.setNGeki(score.getNumKatus());
    performance.setN300(score.getNum300s());
    performance.setN100(score.getNum100s());
    performance.setN50(score.getNum50s());
    performance.setMisses(score.getNumMisses());
    performance.setCombo(score.getHighestCombo());
    PerformanceAttributes attributes = performance.calculate();

    return attributes.pp();
  }

  @AllArgsConstructor
  @Getter
  public static class ParsedScoreDTO {
    public Score score;
    public byte[] replayBytes;
  }
}
