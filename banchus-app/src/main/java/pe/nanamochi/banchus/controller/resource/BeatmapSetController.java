package pe.nanamochi.banchus.controller.resource;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.nanamochi.banchus.service.UserService;

@Slf4j
@RestController
@RequestMapping("/d")
@RequiredArgsConstructor
public class BeatmapSetController {
  private final UserService userService;

  @GetMapping("/{beatmapSetId}")
  public ResponseEntity<String> downloadBeatmapSet(
      @PathVariable String beatmapSetId,
      @RequestParam("u") String username,
      @RequestParam("h") String passwordMd5,
      @RequestParam(value = "vv") int endpointVersion) {
    return userService
        .login(username, passwordMd5)
        .map(
            user -> {
              log.debug("Beatmap download request: user={}, setId={}", username, beatmapSetId);

              String redirectUrl = "https://osu.direct/d/" + beatmapSetId;
              return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                  .location(URI.create(redirectUrl))
                  .body("");
            })
        .orElseGet(
            () -> {
              log.warn("Unauthorized download attempt: user={}", username);
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                  .body("-1\nInvalid username or password.");
            });
  }
}
