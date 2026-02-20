package pe.nanamochi.banchus.service.infra.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.domain.storage.FileStorageProvider; // Import de common

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "banchus.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageProvider implements FileStorageProvider {

  private final String basePath = ".data/";

  @Override
  public void initialize(List<String> buckets) {
    buckets.forEach(
        bucket -> {
          try {
            Path path = Path.of(basePath, bucket);
            if (!Files.exists(path)) {
              Files.createDirectories(path);
              log.info("Storage directory created: {}", path);
            }
          } catch (IOException e) {
            log.error("Critical error initializing local bucket: {}", bucket, e);
            throw new RuntimeException("Could not create directory: " + bucket, e);
          }
        });
  }

  @Override
  public Optional<byte[]> read(String bucket, String key) {
    try {
      Path path = Path.of(basePath, bucket, key);
      return Optional.of(Files.readAllBytes(path));
    } catch (IOException e) {
      log.warn("File not found in local storage: {}/{}", bucket, key);
      return Optional.empty();
    }
  }

  @Override
  public void write(String bucket, String key, byte[] content) {
    try {
      Path path = Path.of(basePath, bucket, key);
      Files.createDirectories(path.getParent());
      Files.write(path, content);
      log.debug("File written locally: {}/{}", bucket, key);
    } catch (IOException e) {
      log.error("Error writing local file: {}/{}", bucket, key, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void delete(String bucket, String key) {
    try {
      Files.deleteIfExists(Path.of(basePath, bucket, key));
    } catch (IOException e) {
      log.error("Could not delete local file: {}/{}", bucket, key);
    }
  }

  @Override
  public boolean exists(String bucket, String key) {
    return Files.exists(Path.of(basePath, bucket, key));
  }

  @Override
  public Path getFileAsPath(String bucket, String key) {
    return Path.of(basePath, bucket, key);
  }
}
