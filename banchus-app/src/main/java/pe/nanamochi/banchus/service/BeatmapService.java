package pe.nanamochi.banchus.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.database.entity.Beatmap;
import pe.nanamochi.banchus.database.entity.Beatmapset;
import pe.nanamochi.banchus.database.repository.BeatmapRepository;
import pe.nanamochi.banchus.domain.dto.OsuApiBeatmap;
import pe.nanamochi.banchus.mapper.BeatmapMapper;
import pe.nanamochi.banchus.service.infra.OsuApiClient;
import pe.nanamochi.banchus.util.Security;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeatmapService {

  private final BeatmapRepository beatmapRepository;
  private final BeatmapMapper beatmapMapper;
  private final StorageService storageService;
  private final OsuApiClient osuApiClient;
  private final BeatmapsetService beatmapsetService;

  public Beatmap save(Beatmap beatmap) {
    return beatmapRepository.save(beatmap);
  }

  public Beatmap save(OsuApiBeatmap apiBeatmap, Beatmapset beatmapset) {
    Beatmap beatmap = beatmapMapper.fromApi(apiBeatmap);
    beatmap.setBeatmapset(beatmapset);
    return save(beatmap);
  }

  public Beatmap update(Beatmap beatmap) {
    if (!beatmapRepository.existsById(beatmap.getId())) {
      throw new IllegalArgumentException("Beatmap not found: " + beatmap.getId());
    }
    return save(beatmap);
  }

  public Optional<Beatmap> findById(int id) {
    return beatmapRepository.findById(id);
  }

  public Optional<Beatmap> findByMd5(String md5) {
    return beatmapRepository.findByMd5(md5);
  }

  public Optional<byte[]> getOrDownloadOsuFile(int beatmapId, String expectedMd5) {
    Optional<byte[]> localFile = storageService.getBeatmap(beatmapId);

    if (localFile.isPresent()) {
      byte[] data = localFile.get();
      if (expectedMd5 == null || Security.getMd5FromBytes(data).equalsIgnoreCase(expectedMd5)) {
        return localFile;
      }
      log.debug("Local .osu file for {} does not match MD5. Redownloading...", beatmapId);
    }

    return osuApiClient
        .getOsuFile(beatmapId)
        .map(
            downloaded -> {
              storageService.uploadBeatmap(beatmapId, downloaded);
              return downloaded;
            });
  }

  public Path getBeatmapPath(int beatmapId) {
    return storageService.getBeatmapPath(beatmapId);
  }

  public Beatmap getOrCreateBeatmap(String beatmapMd5) {
    Beatmap beatmap =
        findByMd5(beatmapMd5)
            .orElseGet(
                () -> {
                  log.debug("Beatmap {} not found. Querying osu!api...", beatmapMd5);

                  OsuApiBeatmap apiData = osuApiClient.getBeatmap(beatmapMd5).orElse(null);
                  if (apiData == null) return null;

                  Beatmapset beatmapset =
                      beatmapsetService
                          .findById(apiData.getBeatmapsetId())
                          .orElseGet(() -> beatmapsetService.save(apiData));

                  List<OsuApiBeatmap> setBeatmaps =
                      osuApiClient.getBeatmaps(apiData.getBeatmapsetId());

                  Beatmap target = null;
                  for (OsuApiBeatmap b : setBeatmaps) {
                    Beatmap current =
                        findByMd5(b.getFileMd5())
                            .orElseGet(
                                () -> {
                                  Beatmap newMap = save(b, beatmapset);
                                  getOrDownloadOsuFile(b.getBeatmapId(), b.getFileMd5());
                                  return newMap;
                                });

                    if (b.getFileMd5().equals(beatmapMd5)) target = current;
                  }
                  return target;
                });

    if (beatmap != null) {
      beatmap = updateBeatmapIfOutdated(beatmap);
    }

    return beatmap;
  }

  private Beatmap updateBeatmapIfOutdated(Beatmap beatmap) {
    List<OsuApiBeatmap> osuApiBeatmaps = osuApiClient.getBeatmaps(beatmap.getBeatmapset().getId());
    if (osuApiBeatmaps.isEmpty()) return beatmap;

    Instant localLastUpdate = beatmap.getBeatmapset().getLastUpdated();
    Instant remoteLastUpdate =
        osuApiBeatmaps.stream()
            .map(OsuApiBeatmap::getLastUpdate)
            .max(Instant::compareTo)
            .orElse(localLastUpdate);

    if (localLastUpdate.isBefore(remoteLastUpdate)) {
      log.debug("Updating beatmapset {} (Outdated)", beatmap.getBeatmapset().getId());

      Beatmapset beatmapset = beatmap.getBeatmapset();
      beatmapset.setLastUpdated(remoteLastUpdate);
      beatmapsetService.save(beatmapset);

      for (OsuApiBeatmap b : osuApiBeatmaps) {
        getOrDownloadOsuFile(b.getBeatmapId(), b.getFileMd5());

        Beatmap local = findByMd5(b.getFileMd5()).orElse(null);
        if (local != null) {
          local.setLastUpdated(b.getLastUpdate());
          local.setStarRating(b.getDifficultyRating());
          update(local);

          if (b.getFileMd5().equals(beatmap.getMd5())) {
            beatmap = local;
          }
        }
      }
    }

    return beatmap;
  }
}
