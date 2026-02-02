package pe.nanamochi.banchus.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.entities.db.Beatmapset;

@Repository
public interface BeatmapsetRepository extends JpaRepository<Beatmapset, Integer> {
  Beatmapset findById(int id);
}
