package pe.nanamochi.banchus.services;

import java.security.MessageDigest;
import java.util.HexFormat;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.entities.StorageType;
import pe.nanamochi.banchus.entities.db.Beatmap;
import pe.nanamochi.banchus.mappers.BeatmapMapper;
import pe.nanamochi.banchus.repositories.BeatmapRepository;
import pe.nanamochi.banchus.utils.OsuApi;

@Service
@AllArgsConstructor
public class BeatmapService {

  private final BeatmapRepository beatmapRepository;
  private final BeatmapMapper beatmapMapper;
  private final FileStorageService storage;
  private final OsuApi osuApi;

  public Beatmap create(Beatmap beatmap) {
    return beatmapRepository.save(beatmap);
  }

  public Beatmap createFromApi(pe.nanamochi.banchus.entities.osuapi.Beatmap beatmap) {
    return beatmapMapper.fromApi(beatmap);
  }

  public Beatmap update(Beatmap beatmap) {
    if (!beatmapRepository.existsById(beatmap.getId())) {
      throw new IllegalArgumentException("Beatmap not found: " + beatmap.getId());
    }
    return beatmapRepository.save(beatmap);
  }

  public Beatmap findByBeatmapId(int beatmapId) {
    return beatmapRepository.findById(beatmapId);
  }

  public Beatmap findByMd5(String md5) {
    return beatmapRepository.findByMd5(md5);
  }

  public byte[] getOrDownloadOsuFile(int beatmapId, String expectedMd5) {
    String fileKey = String.valueOf(beatmapId);

    if (storage.exists(StorageType.OSU, fileKey)) {
      byte[] localFile = storage.read(StorageType.OSU, fileKey);

      if (localFile != null) {
        if (expectedMd5 == null || calculateMd5(localFile).equalsIgnoreCase(expectedMd5)) {
          return localFile;
        }
      }
    }

    byte[] downloaded = osuApi.getOsuFile(beatmapId);
    if (downloaded == null) {
      return null;
    }

    storage.write(StorageType.OSU, fileKey, downloaded);
    return downloaded;
  }

  private String calculateMd5(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      return HexFormat.of().formatHex(md.digest(data));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute MD5", e);
    }
  }
}
