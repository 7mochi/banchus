package pe.nanamochi.banchus.service;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.database.entity.Score;
import pe.nanamochi.banchus.database.entity.Stat;
import pe.nanamochi.banchus.database.entity.User;
import pe.nanamochi.banchus.database.repository.StatRepository;
import pe.nanamochi.banchus.domain.enums.Mode;

@Service
@RequiredArgsConstructor
public class StatService {
  private final StatRepository statRepository;

  private static final double DECAY = 0.95;

  @Transactional
  public List<Stat> createAllGamemodes(User user) {
    List<Stat> stats = new ArrayList<>();
    for (Mode mode : Mode.values()) {
      Stat stat =
          Stat.builder()
              .user(user)
              .gamemode(mode)
              .rankedScore(0L)
              .totalScore(0L)
              .accuracy(0.0)
              .playCount(0)
              .performancePoints(0)
              .build();
      stats.add(statRepository.save(stat));
    }
    return stats;
  }

  public Optional<Stat> findByUserAndGamemode(User user, Mode gamemode) {
    return statRepository.findByUserAndGamemode(user, gamemode);
  }

  public Stat update(Stat stat) {
    if (!statRepository.existsById(stat.getId())) {
      throw new IllegalArgumentException("Stat not found: " + stat.getId());
    }
    return statRepository.save(stat);
  }

  public double calculateWeightedAccuracy(List<Score> topScores) {
    double weightedSum = 0;
    double bonusSum = 0;

    for (int i = 0; i < topScores.size(); i++) {
      double weight = Math.pow(DECAY, i);
      weightedSum += (topScores.get(i).getAccuracy() * weight);
      bonusSum += weight;
    }

    return bonusSum == 0 ? 0 : weightedSum / bonusSum;
  }

  public double calculateWeightedPp(List<Score> topScores) {
    double weightedPp = 0;
    for (int i = 0; i < topScores.size(); i++) {
      weightedPp += (topScores.get(i).getPerformancePoints() * Math.pow(DECAY, i));
    }

    return Math.round(weightedPp);
  }
}
