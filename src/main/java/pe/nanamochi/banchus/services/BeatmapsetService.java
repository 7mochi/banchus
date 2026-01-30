package pe.nanamochi.banchus.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.entities.db.Beatmapset;
import pe.nanamochi.banchus.repositories.BeatmapsetRepository;

@Service
public class BeatmapsetService {
  @Autowired private BeatmapsetRepository beatmapsetRepository;
  @Autowired private BeatmapService beatmapService;

  public Beatmapset create(Beatmapset beatmapset) {
    return beatmapsetRepository.save(beatmapset);
  }

  public Beatmapset update(Beatmapset beatmapset) {
    if (!beatmapsetRepository.existsById(beatmapset.getId())) {
      throw new IllegalArgumentException("Beatmapset not found: " + beatmapset.getId());
    }
    return beatmapsetRepository.save(beatmapset);
  }

  public Beatmapset findByBeatmapsetId(int beatmapsetId) {
    return beatmapsetRepository.findById(beatmapsetId);
  }
}
