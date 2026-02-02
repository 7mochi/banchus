package pe.nanamochi.banchus.startup;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DataDirectoryInitializer {

  private static final Logger logger = LoggerFactory.getLogger(DataDirectoryInitializer.class);

  public static final Path DATA_DIR = Path.of(".data");

  public static final Path OSU_DIR = DATA_DIR.resolve("osu_beatmap_files");
  public static final Path REPLAYS_DIR = DATA_DIR.resolve("replays_files");
  public static final Path SCREENSHOTS_DIR = DATA_DIR.resolve("screenshots_files");

  private static final List<Path> REQUIRED_DIRS =
      List.of(DATA_DIR, OSU_DIR, REPLAYS_DIR, SCREENSHOTS_DIR);

  @PostConstruct
  public void init() {
    try {
      for (Path dir : REQUIRED_DIRS) {
        boolean existed = Files.exists(dir);
        Files.createDirectories(dir);

        logger.info(
            existed ? "Directory already exists: {}, skipping creation" : "Created directory: {}",
            dir.toAbsolutePath());
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize data directories", e);
    }
  }
}
