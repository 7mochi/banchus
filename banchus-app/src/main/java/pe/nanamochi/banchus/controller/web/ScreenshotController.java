package pe.nanamochi.banchus.controller.web;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.nanamochi.banchus.service.StorageService;
import pe.nanamochi.banchus.service.UserService;

@Slf4j
@RestController
@RequestMapping("/web")
@RequiredArgsConstructor
public class ScreenshotController {
  private final UserService userService;
  private final StorageService storageService;

  @PostMapping("/osu-screenshot.php")
  public ResponseEntity<String> uploadScreenshot(
      @RequestParam(value = "u", required = false) String username,
      @RequestParam(value = "p", required = false) String passwordMd5,
      @RequestParam(value = "v", required = false) Integer endpointVersion,
      @RequestParam(value = "ss", required = false) MultipartFile screenshotFile) {
    return userService
        .login(username, passwordMd5)
        .map(
            user -> {
              if (screenshotFile == null || screenshotFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Empty file");
              }

              if (screenshotFile.getSize() > 4 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body("File too large");
              }

              if (endpointVersion != null && endpointVersion != 1) {
                log.warn(
                    "Incorrect endpoint version for {}: v{}", user.getUsername(), endpointVersion);
              }

              try {
                String filename = storageService.saveScreenshot(screenshotFile.getBytes());
                log.debug("Screenshot saved: {} by user: {}", filename, user.getUsername());
                return ResponseEntity.ok(filename);
              } catch (IOException e) {
                log.error("Physical error saving screenshot from {}", user.getUsername(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<String>build();
              }
            })
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }
}
