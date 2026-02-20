package pe.nanamochi.banchus.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.nanamochi.banchus.service.ScoreService;
import pe.nanamochi.banchus.service.SessionService;
import pe.nanamochi.banchus.service.StorageService;
import pe.nanamochi.banchus.service.UserService;

@Slf4j
@RestController
@RequestMapping("/web")
@RequiredArgsConstructor
public class ReplayController {
  private final UserService userService;
  private final SessionService sessionService;
  private final ScoreService scoreService;
  private final StorageService storageService;

  @GetMapping(value = "/osu-getreplay.php", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> getReplay(
      @RequestParam(value = "u") String username,
      @RequestParam(value = "h") String passwordMd5,
      @RequestParam(value = "c") Integer scoreId) {
    return userService
        .login(username, passwordMd5)
        .map(
            user -> {
              if (sessionService.findPrimaryByUsername(user.getUsername()).isEmpty()) {
                log.warn("Replay request for {} without active session.", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).<byte[]>build();
              }

              return scoreService
                  .findById(scoreId)
                  .flatMap(score -> storageService.getReplay(scoreId))
                  .map(
                      replayData -> {
                        log.debug("Serving replay ID: {} to user: {}", scoreId, username);

                        // TODO: increment replay views for this score, there are things to
                        // consider like:
                        // - dont increase views fore the player watching their own replay
                        // - manage a cooldown so people cant just spam refresh to increase views
                        // (use redis for this)

                        return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(replayData);
                      })
                  .orElseGet(
                      () -> {
                        log.error("Score or physical file not found for ID: {}", scoreId);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                      });
            })
        .orElseGet(
            () -> {
              log.warn("Unauthorized replay access attempt by user: {}", username);
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            });
  }
}
