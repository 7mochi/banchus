package pe.nanamochi.banchus.service.infra.performance;

import pe.nanamochi.banchus.database.entity.Score;

public interface PerformanceCalculator {
  double calculate(String beatmapPath, Score score) throws Exception;

  boolean supports(CalculatorType type);
}
