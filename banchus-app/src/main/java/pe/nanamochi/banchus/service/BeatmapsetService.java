package pe.nanamochi.banchus.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.database.entity.Beatmapset;
import pe.nanamochi.banchus.database.repository.BeatmapsetRepository;
import pe.nanamochi.banchus.domain.dto.OsuApiBeatmap;
import pe.nanamochi.banchus.mapper.BeatmapsetMapper;

@Service
@RequiredArgsConstructor
public class BeatmapsetService {
  private final BeatmapsetRepository beatmapsetRepository;
  private final BeatmapsetMapper beatmapsetMapper;

  public Beatmapset save(Beatmapset beatmapset) {
    return beatmapsetRepository.save(beatmapset);
  }

  public Beatmapset save(OsuApiBeatmap apiBeatmap) {
    Beatmapset beatmapset = beatmapsetMapper.fromApi(apiBeatmap);
    return save(beatmapset);
  }

  public Beatmapset update(Beatmapset beatmapset) {
    if (!beatmapsetRepository.existsById(beatmapset.getId())) {
      throw new IllegalArgumentException("Beatmapset not found: " + beatmapset.getId());
    }
    return beatmapsetRepository.save(beatmapset);
  }

  public Optional<Beatmapset> findById(int id) {
    return beatmapsetRepository.findById(id);
  }
}
