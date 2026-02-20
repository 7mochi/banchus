package pe.nanamochi.banchus.controller.resource;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nanamochi.banchus.service.StorageService;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class AvatarController {
  private final StorageService storageService;

  @GetMapping(value = "/{userId}", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> getAvatar(@PathVariable String userId) {
    byte[] avatarData = storageService.getAvatar(userId);
    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(avatarData);
  }
}
