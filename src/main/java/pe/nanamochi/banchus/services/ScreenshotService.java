package pe.nanamochi.banchus.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.config.StorageConfig;
import pe.nanamochi.banchus.utils.Security;

@Service
@RequiredArgsConstructor
public class ScreenshotService {
  private final FileStorageService storage;

  public String saveScreenshot(byte[] screenshotBytes) {
    String filename = Security.generateToken(6);
    storage.write(StorageConfig.SCREENSHOT, filename, screenshotBytes);
    return filename;
  }

  public byte[] getScreenshot(String screenshotId) {
    return storage.read(StorageConfig.SCREENSHOT, screenshotId);
  }
}
