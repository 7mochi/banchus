package pe.nanamochi.banchus.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.nanamochi.banchus.domain.enums.BeatmapDirectDisplayMode;
import pe.nanamochi.banchus.service.UserService;
import pe.nanamochi.banchus.service.infra.OsuDirectApiClient;

@Slf4j
@RestController
@RequestMapping("/web")
@RequiredArgsConstructor
public class DirectController {
  private final OsuDirectApiClient osuDirectApiService;
  private final UserService userService;

  @GetMapping(value = "/osu-search.php", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> osuSearchHandler(
      @RequestParam("u") String username,
      @RequestParam("h") String passwordMd5,
      @RequestParam("r") BeatmapDirectDisplayMode displayMode,
      @RequestParam("p") int pageOffset,
      @RequestParam("q") String query,
      @RequestParam("m") int mode) {

    return userService
        .login(username, passwordMd5)
        .map(
            _ -> {
              log.debug("osu!direct search request: user={}, query='{}'", username, query);

              return osuDirectApiService
                  .search(query, mode, displayMode, pageOffset)
                  .map(ResponseEntity::ok)
                  .orElseGet(
                      () -> {
                        log.warn("osu!direct search returned no data for query: {}", query);
                        return ResponseEntity.ok(
                            "-1\nFailed to retrieve data from the beatmap mirror.");
                      });
            })
        .orElseGet(
            () -> {
              log.warn("osu!direct unauthorized access attempt: user={}", username);
              return ResponseEntity.status(401).body("-1\nInvalid username or password.");
            });
  }
}
