package pe.nanamochi.banchus.repositories.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.entities.db.Beatmap;

@Repository
public interface BeatmapRepository extends JpaRepository<Beatmap, Integer> {
  Beatmap findById(int id);

  Beatmap findByMd5(String md5);
}
