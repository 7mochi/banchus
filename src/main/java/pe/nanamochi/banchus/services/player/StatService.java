package pe.nanamochi.banchus.services.player;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.entities.commons.Mode;
import pe.nanamochi.banchus.entities.db.Score;
import pe.nanamochi.banchus.entities.db.Stat;
import pe.nanamochi.banchus.entities.db.User;
import pe.nanamochi.banchus.repositories.db.StatRepository;

@Service
@RequiredArgsConstructor
public class StatService {
  private final StatRepository statRepository;

  private static final double DECAY = 0.95;

  public List<Stat> createAllGamemodes(User user) {
    // 0 = standard
    // 1 = taiko
    // 2 = catch
    // 3 = mania
    List<Stat> stats = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      Stat stat = new Stat();
      stat.setUser(user);
      stat.setGamemode(Mode.fromValue(i));
      stats.add(statRepository.save(stat));
    }
    return stats;
  }

  public Stat update(Stat stat) {
    if (!statRepository.existsById(stat.getId())) {
      throw new IllegalArgumentException("Stat not found: " + stat.getId());
    }
    return statRepository.save(stat);
  }

  public Stat getStats(User user, Mode gamemode) {
    return statRepository.findByUserAndGamemode(user, gamemode);
  }

  public double calculateWeightedAccuracy(List<Score> topScores) {
    double result = 0;
    for (int i = 0; i < topScores.size(); i++) {
      result += (topScores.get(i).getAccuracy() * Math.pow(DECAY, i));
    }
    return result;
  }

  public double calculateWeightedPp(List<Score> topScores) {
    double result = 0;
    for (int i = 0; i < topScores.size(); i++) {
      result += (topScores.get(i).getPerformancePoints() * Math.pow(DECAY, i));
    }
    return result;
  }
}
