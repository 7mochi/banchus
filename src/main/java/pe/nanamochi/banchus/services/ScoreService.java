package pe.nanamochi.banchus.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.entities.db.Score;
import pe.nanamochi.banchus.repositories.ScoreRepository;

@Service
public class ScoreService {
  @Autowired private ScoreRepository scoreRepository;

  public Score saveScore(Score score) {
    return scoreRepository.save(score);
  }
}
