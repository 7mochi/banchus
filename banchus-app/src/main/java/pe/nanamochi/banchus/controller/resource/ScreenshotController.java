package pe.nanamochi.banchus.controller.resource;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.nanamochi.banchus.service.StorageService;

@RestController("ResourceScreenshotController")
@RequestMapping("/ss")
@RequiredArgsConstructor
public class ScreenshotController {
  private final StorageService storageService;

  @GetMapping("/{screenshotId}")
  public ResponseEntity<?> getScreenshot(@PathVariable String screenshotId) {
    return storageService
        .getScreenshot(screenshotId)
        .<ResponseEntity<?>>map(
            data -> ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(data))
        .orElseGet(
            () ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\": \"Screenshot not found.\"}"));
  }
}
