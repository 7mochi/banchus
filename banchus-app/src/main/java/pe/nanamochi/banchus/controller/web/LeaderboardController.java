package pe.nanamochi.banchus.controller.web;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.nanamochi.banchus.database.entity.Score;
import pe.nanamochi.banchus.domain.enums.*;
import pe.nanamochi.banchus.service.BeatmapService;
import pe.nanamochi.banchus.service.ScoreService;
import pe.nanamochi.banchus.service.UserService;

@Slf4j
@RestController
@RequestMapping("/web")
@RequiredArgsConstructor
public class LeaderboardController {
  private final UserService userService;
  private final BeatmapService beatmapService;
  private final ScoreService scoreService;

  @GetMapping(value = "/osu-osz2-getscores.php", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> getScores(
      @RequestParam(name = "us") String username,
      @RequestParam(name = "ha") String passwordMd5,
      @RequestParam(name = "v") Integer leaderboardTypeVal,
      @RequestParam(name = "c") String beatmapMd5,
      @RequestParam(name = "m") Integer gamemode,
      @RequestParam(name = "mods") Integer modsBitmask,
      @RequestParam(name = "f") String filename,
      @RequestParam(name = "i") Integer beatmapSetId,
      @RequestParam(name = "vv") Integer version,
      @RequestParam(name = "s") String skip,
      @RequestParam(name = "h") String hash,
      @RequestParam(name = "a") String aqn) {

    return userService
        .login(username, passwordMd5)
        .map(
            user -> {
              log.debug("Ranking request: user={}, map={}", username, beatmapMd5);

              return Optional.ofNullable(beatmapService.getOrCreateBeatmap(beatmapMd5))
                  .map(
                      beatmap -> {
                        LeaderboardType type = LeaderboardType.fromValue(leaderboardTypeVal);
                        Integer modsToFilter = (type == LeaderboardType.MODS) ? modsBitmask : null;
                        CountryCode country =
                            (type == LeaderboardType.COUNTRY) ? user.getCountry() : null;

                        List<Score> scores =
                            scoreService.fetchBeatmapLeaderboard(
                                beatmap,
                                Mode.fromValue(gamemode),
                                modsToFilter,
                                SubmissionStatus.BEST,
                                country);

                        return ResponseEntity.ok(
                            scoreService.formatLeaderboardResponse(
                                scores,
                                scoreService.findBest(beatmap, user).orElse(null),
                                user,
                                beatmap));
                      })
                  .orElseGet(
                      () -> {
                        log.debug("Map not available: {}", beatmapMd5);
                        return ResponseEntity.ok("-1|false");
                      });
            })
        .orElseGet(
            () -> {
              log.warn("Authentication failed: {}", username);
              return ResponseEntity.status(401).build();
            });
  }
}
