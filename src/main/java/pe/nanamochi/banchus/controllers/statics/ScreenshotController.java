package pe.nanamochi.banchus.controllers.statics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.nanamochi.banchus.services.infra.ScreenshotService;
import pe.nanamochi.banchus.utils.Media;

@RestController("StaticScreenshotController")
@RequestMapping("/")
@RequiredArgsConstructor
public class ScreenshotController {
  private final ScreenshotService screenshotService;

  @GetMapping("/ss/{screenshotId}")
  public ResponseEntity<?> getScreenshot(@PathVariable String screenshotId) {
    byte[] screenshotData = screenshotService.getScreenshot(screenshotId);

    if (screenshotData == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .contentType(MediaType.APPLICATION_JSON)
          .body("{\"status\": \"Screenshot not found.\"}");
    }
    // TODO: idk if its jpg or png :sob:
    return ResponseEntity.ok()
        .contentType(Media.getImageMediaType(screenshotData))
        .body(screenshotData);
  }
}
