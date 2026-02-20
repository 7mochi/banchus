package pe.nanamochi.banchus.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.nanamochi.banchus.domain.dto.ParsedScore;
import pe.nanamochi.banchus.domain.enums.ScoreSubmissionErrors;
import pe.nanamochi.banchus.service.BeatmapService;
import pe.nanamochi.banchus.service.ScoreService;
import pe.nanamochi.banchus.service.SessionService;
import pe.nanamochi.banchus.service.UserService;
import pe.nanamochi.banchus.service.score.ScoreParser; // Tu nuevo componente

@Slf4j
@RestController
@RequestMapping("/web")
@RequiredArgsConstructor
public class ScoringController {
  private final UserService userService;
  private final SessionService sessionService;
  private final ScoreService scoreService;
  private final BeatmapService beatmapService;
  private final ScoreParser scoreParser;

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
      @RequestPart(value = "i", required = false) MultipartFile screenshot) {
    try {
      ParsedScore parsedScore = scoreParser.parse(request, ivB64, osuVersion, scoreTime);

      String username = parsedScore.username();
      String beatmapMd5 = parsedScore.beatmapMd5();

      return userService
          .login(username, passwordMd5)
          .map(
              user ->
                  sessionService
                      .findPrimaryByUsername(user.getUsername())
                      .map(
                          session ->
                              beatmapService
                                  .findByMd5(beatmapMd5)
                                  .map(
                                      beatmap -> {
                                        try {
                                          return scoreService.processScoreSubmission(
                                              parsedScore, user, beatmap, session);
                                        } catch (Exception e) {
                                          log.error(
                                              "Error processing submission for {}: ", username, e);
                                          return "error: no";
                                        }
                                      })
                                  .orElse(
                                      "error: "
                                          + ScoreSubmissionErrors.BEATMAP_UNRANKED.getValue()))
                      .orElse("error: " + ScoreSubmissionErrors.NEEDS_AUTHENTICATION.getValue()))
          .orElse("error: " + ScoreSubmissionErrors.NEEDS_AUTHENTICATION.getValue());

    } catch (Exception e) {
      log.error("Error parsing score: ", e);
      return "error: " + ScoreSubmissionErrors.NEEDS_AUTHENTICATION.getValue();
    }
  }
}
