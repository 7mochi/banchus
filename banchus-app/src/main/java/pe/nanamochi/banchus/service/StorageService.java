package pe.nanamochi.banchus.service; // O pe.nanamochi.banchus.service.infra según prefieras

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.domain.storage.FileStorageProvider;
import pe.nanamochi.banchus.util.Security;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
  private final FileStorageProvider provider;

  private static final String AVATARS = "avatars_files";
  private static final String BEATMAPS = "osu_beatmap_files";
  private static final String REPLAYS = "replays_files";
  private static final String SCREENSHOTS = "screenshots_files";

  public static final List<String> ALL_BUCKETS = List.of(AVATARS, BEATMAPS, REPLAYS, SCREENSHOTS);

  public void initStorage() throws IOException {
    provider.initialize(ALL_BUCKETS);

    if (!provider.exists(AVATARS, "default.png")) {
      try (var is = getClass().getResourceAsStream("/images/default.png")) {
        if (is != null) {
          provider.write(AVATARS, "default.png", is.readAllBytes());
          log.info("Default avatar initialized.");
        }
      } catch (IOException e) {
        log.error("Failed to setup default avatar", e);
      }
    }
  }

  public byte[] getAvatar(String userId) {
    return provider
        .read(AVATARS, userId + ".png")
        .or(() -> provider.read(AVATARS, "default.png"))
        .orElse(new byte[0]);
  }

  public void uploadAvatar(String userId, byte[] content) {
    provider.write(AVATARS, userId + ".png", content);
  }

  public Optional<byte[]> getBeatmap(int beatmapId) {
    return provider.read(BEATMAPS, beatmapId + ".osu");
  }

  public void uploadBeatmap(int beatmapId, byte[] content) {
    provider.write(BEATMAPS, beatmapId + ".osu", content);
  }

  public boolean beatmapExists(int beatmapId) {
    return provider.exists(BEATMAPS, beatmapId + ".osu");
  }

  public Path getBeatmapPath(int beatmapId) {
    return provider.getFileAsPath(BEATMAPS, beatmapId + ".osu");
  }

  public Optional<byte[]> getReplay(long scoreId) {
    return provider.read(REPLAYS, scoreId + ".osr");
  }

  public void saveReplay(long scoreId, byte[] content) {
    provider.write(REPLAYS, scoreId + ".osr", content);
  }

  public Optional<byte[]> getScreenshot(String screenshotId) {
    return provider.read(SCREENSHOTS, screenshotId + ".png");
  }

  public String saveScreenshot(byte[] content) {
    String id = Security.generateToken(6);
    provider.write(SCREENSHOTS, id + ".png", content);
    return id;
  }
}
