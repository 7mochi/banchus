package pe.nanamochi.banchus.entities;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Getter;
import pe.nanamochi.banchus.startup.DataDirectoryInitializer;

@Getter
@AllArgsConstructor
public enum StorageType {
  OSU(DataDirectoryInitializer.OSU_DIR, ".osu"),
  REPLAY(DataDirectoryInitializer.REPLAYS_DIR, ".osr"),
  SCREENSHOT(DataDirectoryInitializer.SCREENSHOTS_DIR, ".png");

  private final Path baseDir;
  private final String extension;

  public Path resolve(String name) {
    return baseDir.resolve(name + extension);
  }

  public Path dir() {
    return baseDir;
  }
}
