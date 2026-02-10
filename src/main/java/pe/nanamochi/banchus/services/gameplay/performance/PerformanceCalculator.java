package pe.nanamochi.banchus.services.gameplay.performance;

import pe.nanamochi.banchus.entities.db.Score;

public interface PerformanceCalculator {
  double calculate(String beatmapPath, Score score) throws Exception;

  boolean supports(CalculatorType type);
}
