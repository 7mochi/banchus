package pe.nanamochi.banchus.services;

import java.util.List;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.entities.db.Score;

@Service
public class UserStatCalculationService {
  private static final float DECAY = 0.95f;

  public float calculateWeightedAccuracy(List<Score> topScores) {
    float result = 0f;
    for (int i = 0; i < topScores.size(); i++) {
      result += (float) (topScores.get(i).getAccuracy() * Math.pow(DECAY, i));
    }
    return result;
  }

  public float calculateWeightedPp(List<Score> topScores) {
    float result = 0f;
    for (int i = 0; i < topScores.size(); i++) {
      result += (float) (topScores.get(i).getPerformancePoints() * Math.pow(DECAY, i));
    }
    return result;
  }
}
