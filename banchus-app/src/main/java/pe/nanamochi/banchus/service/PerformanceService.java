package pe.nanamochi.banchus.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.database.entity.Score;
import pe.nanamochi.banchus.service.infra.performance.CalculatorType;
import pe.nanamochi.banchus.service.infra.performance.PerformanceCalculator;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceService {
  private final List<PerformanceCalculator> calculators;

  @Value("${banchus.pp-calculator-type:rosu-pp}")
  private String calculatorType;

  public double calculate(String beatmapPath, Score score) throws Exception {
    CalculatorType type = CalculatorType.fromAlias(calculatorType);
    return calculate(beatmapPath, score, type);
  }

  public double calculate(String beatmapPath, Score score, CalculatorType type) throws Exception {
    log.debug("Calculating PP for score in {} using calculator: {}", beatmapPath, type);

    return calculators.stream()
        .filter(c -> c.supports(type))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("No implementation found for calculator: " + type))
        .calculate(beatmapPath, score);
  }
}
