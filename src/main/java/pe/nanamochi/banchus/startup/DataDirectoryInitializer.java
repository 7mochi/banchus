package pe.nanamochi.banchus.startup;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.config.StorageConfig;

@Component
public class DataDirectoryInitializer {
  private static final Logger logger = LoggerFactory.getLogger(DataDirectoryInitializer.class);

  @PostConstruct
  public void init() {
    try {
      for (StorageConfig config : StorageConfig.values()) {
        Path dir = config.getBaseDir();

        if (!Files.exists(dir)) {
          Files.createDirectories(dir);
          logger.info("Created directory: {}", dir.toAbsolutePath());
        } else {
          logger.info("Directory already exists: {}", dir);
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize data directories", e);
    }
  }
}
