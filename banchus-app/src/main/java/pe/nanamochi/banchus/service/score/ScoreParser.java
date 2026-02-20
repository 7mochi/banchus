package pe.nanamochi.banchus.service.score;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.database.entity.Score;
import pe.nanamochi.banchus.domain.dto.ParsedScore;
import pe.nanamochi.banchus.domain.enums.Mode;
import pe.nanamochi.banchus.util.Rijndael;

@Component
@RequiredArgsConstructor
public class ScoreParser {
  public ParsedScore parse(
      HttpServletRequest request, String ivB64, String osuVersion, Integer scoreTime)
      throws Exception {
    byte[] iv = Base64.getDecoder().decode(ivB64);

    // The bancho protocol uses the "score" parameter name for both the base64'ed score data,
    // and the replay file in the multipart. @RequestPart can´t handle it well, so we manually
    // handle it here with HttpServletRequest
    List<Part> scoreParts =
        request.getParts().stream().filter(p -> p.getName().equals("score")).toList();

    String scoreDataAesB64 =
        new String(scoreParts.get(0).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    byte[] replayBytes = scoreParts.get(1).getInputStream().readAllBytes();

    // Ensure AES key is exactly 32 bytes
    byte[] aesKey =
        String.format("%-32s", "osu!-scoreburgr---------" + osuVersion)
            .substring(0, 32)
            .getBytes(StandardCharsets.UTF_8);

    // Decrypt score data
    byte[] decryptedBytes =
        Rijndael.decrypt(Base64.getDecoder().decode(scoreDataAesB64), aesKey, iv);
    String[] data = new String(decryptedBytes, StandardCharsets.UTF_8).split(":");

    Score score =
        Score.builder()
            .onlineChecksum(data[2])
            .score(Long.parseLong(data[9]))
            .highestCombo(Integer.parseInt(data[10]))
            .fullCombo("1".equals(data[11]) || "True".equalsIgnoreCase(data[11]))
            .mods(Integer.parseInt(data[13]))
            .num300s(Integer.parseInt(data[3]))
            .num100s(Integer.parseInt(data[4]))
            .num50s(Integer.parseInt(data[5]))
            .numMisses(Integer.parseInt(data[8]))
            .numGekis(Integer.parseInt(data[6]))
            .numKatus(Integer.parseInt(data[7]))
            .grade(data[12])
            .mode(Mode.fromValue(Integer.parseInt(data[15])))
            .passed("1".equals(data[14]) || "True".equalsIgnoreCase(data[14]))
            .timeElapsed(scoreTime)
            .build();

    return new ParsedScore(score, replayBytes, data[0], data[1].stripTrailing());
  }
}
