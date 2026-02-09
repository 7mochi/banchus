package pe.nanamochi.banchus.services.gameplay;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.config.StorageConfig;
import pe.nanamochi.banchus.services.infra.FileStorageService;

@Service
@RequiredArgsConstructor
public class ReplayService {
  private final FileStorageService storage;

  public void saveReplay(long scoreId, byte[] replayBytes) {
    storage.write(StorageConfig.REPLAY, String.valueOf(scoreId), replayBytes);
  }

  public byte[] getReplay(long scoreId) {
    return storage.read(StorageConfig.REPLAY, String.valueOf(scoreId));
  }
}
