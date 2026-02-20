package pe.nanamochi.banchus.database.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.database.entity.Beatmapset;

@Repository
public interface BeatmapsetRepository extends JpaRepository<Beatmapset, Integer> {
  Optional<Beatmapset> findById(int id);
}
