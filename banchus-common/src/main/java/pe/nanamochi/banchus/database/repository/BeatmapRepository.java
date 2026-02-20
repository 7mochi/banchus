package pe.nanamochi.banchus.database.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.database.entity.Beatmap;

@Repository
public interface BeatmapRepository extends JpaRepository<Beatmap, Integer> {
  Optional<Beatmap> findById(int id);

  Optional<Beatmap> findByMd5(String md5);
}
