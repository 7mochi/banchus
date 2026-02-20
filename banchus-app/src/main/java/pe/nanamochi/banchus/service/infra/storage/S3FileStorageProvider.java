package pe.nanamochi.banchus.service.infra.storage;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.domain.storage.FileStorageProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "banchus.storage.type", havingValue = "s3")
public class S3FileStorageProvider implements FileStorageProvider {
  private final S3Client s3Client;
  private static final String CACHE_ROOT = "banchus_storage_cache";

  @Value("${banchus.storage.s3.bucket}")
  private String bucketName;

  @Override
  public void initialize(List<String> buckets) throws IOException {
    log.info("Inicializando S3 Storage en bucket: {}", bucketName);
    cleanCache();
  }

  @Override
  public Optional<byte[]> read(String bucket, String key) {
    try {
      byte[] data =
          s3Client.getObject(b -> b.bucket(bucketName).key(bucket + "/" + key)).readAllBytes();
      return Optional.of(data);
    } catch (Exception e) {
      log.warn("Error leyendo de S3: {}/{} (Tal vez no existe)", bucket, key);
      return Optional.empty();
    }
  }

  @Override
  public void write(String bucket, String key, byte[] content) {
    try {
      s3Client.putObject(
          b -> b.bucket(bucketName).key(bucket + "/" + key), RequestBody.fromBytes(content));
      log.debug("Archivo subido a S3: {}/{}", bucket, key);
    } catch (Exception e) {
      log.error("Error al subir archivo a S3: {}/{}", bucket, key, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void delete(String bucket, String key) {
    s3Client.deleteObject(b -> b.bucket(bucketName).key(bucket + "/" + key));
  }

  @Override
  public boolean exists(String bucket, String key) {
    try {
      s3Client.headObject(b -> b.bucket(bucketName).key(bucket + "/" + key));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public Path getFileAsPath(String bucket, String key) {
    Path cachePath = Path.of(System.getProperty("java.io.tmpdir"), CACHE_ROOT, bucket, key);

    if (Files.exists(cachePath)) {
      return cachePath;
    }

    try {
      Files.createDirectories(cachePath.getParent());

      byte[] data =
          this.read(bucket, key)
              .orElseThrow(
                  () -> new RuntimeException("File not found in S3: " + bucket + "/" + key));

      Files.write(cachePath, data);
      return cachePath;
    } catch (IOException e) {
      log.error("Error creating local cache for S3 file: {}", key, e);
      throw new RuntimeException("Failed to cache S3 file locally", e);
    }
  }

  @PreDestroy
  public void cleanCache() throws IOException {
    Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), CACHE_ROOT);
    if (Files.exists(tempDir)) {
      log.info("Cleaning temporary storage cache...");
      try (var stream = Files.walk(tempDir)) {
        stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }
}
