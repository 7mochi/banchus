package pe.nanamochi.banchus.services.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.config.StorageConfig;

@Service
public class FileStorageService {
  public boolean exists(StorageConfig type, String name) {
    return Files.exists(type.resolve(name));
  }

  public Path getPath(StorageConfig type, String name) {
    return type.resolve(name);
  }

  public void write(StorageConfig type, String name, byte[] data) {
    Path path = type.resolve(name);
    try {
      Files.write(path, data);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write file: " + path, e);
    }
  }

  public byte[] read(StorageConfig type, String name) {
    Path path = type.resolve(name);
    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      return null;
    }
  }
}
