package pe.nanamochi.banchus.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.entities.db.Score;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {}
