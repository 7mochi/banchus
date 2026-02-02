package pe.nanamochi.banchus.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.entities.StorageType;

@Service
public class FileStorageService {
  private static final Path ROOT = Path.of(".data");

  public boolean exists(StorageType type, String name) {
    return Files.exists(resolve(type, name));
  }

  public Path getPath(StorageType type, String name) {
    return resolve(type, name);
  }

  public void write(StorageType type, String name, byte[] data) {
    Path path = resolve(type, name);
    try {
      Files.write(path, data);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write file " + path, e);
    }
  }

  public byte[] read(StorageType type, String name) {
    Path path = resolve(type, name);
    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      return null;
    }
  }

  private Path resolve(StorageType type, String name) {
    return type.getBaseDir().resolve(name + type.getExtension());
  }
}
