package pe.nanamochi.banchus.services.gameplay;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.entities.db.Score;
import pe.nanamochi.banchus.services.gameplay.performance.CalculatorType;
import pe.nanamochi.banchus.services.gameplay.performance.PerformanceCalculator;

@Service
@RequiredArgsConstructor
public class PerformanceService {
  private final List<PerformanceCalculator> calculators;

  public double calculate(String beatmapPath, Score score, CalculatorType type) throws Exception {
    return calculators.stream()
        .filter(c -> c.supports(type))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported calculator type: " + type))
        .calculate(beatmapPath, score);
  }
}
