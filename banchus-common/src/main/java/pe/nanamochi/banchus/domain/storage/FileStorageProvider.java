package pe.nanamochi.banchus.domain.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface FileStorageProvider {
  void initialize(List<String> buckets) throws IOException;

  Optional<byte[]> read(String bucket, String key);

  void write(String bucket, String key, byte[] content);

  void delete(String bucket, String key);

  boolean exists(String bucket, String key);

  Path getFileAsPath(String bucket, String key);
}
