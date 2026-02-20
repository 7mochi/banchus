package pe.nanamochi.banchus.startup;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.service.StorageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataDirectoryInitializer {
  private final StorageService storageService;

  @PostConstruct
  public void init() {
    try {
      log.info("Initializing storage infrastructure...");
      storageService.initStorage();
      log.info("Data directories initialized successfully.");
    } catch (IOException e) {
      log.error("Could not initialize data directories: {}", e.getMessage());
      throw new RuntimeException("FileSystem initialization failed", e);
    }
  }
}
