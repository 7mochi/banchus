package pe.nanamochi.banchus.services.gameplay.performance;

import io.github.nanamochi.rosu_pp_jar.GameMode;
import io.github.nanamochi.rosu_pp_jar.Performance;
import io.github.nanamochi.rosu_pp_jar.RosuException;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.entities.commons.Mods;
import pe.nanamochi.banchus.entities.db.Score;

@Component
public class RosuCalculator implements PerformanceCalculator {
  @Override
  public double calculate(String beatmapPath, Score score) throws RosuException {
    var rosuBeatmap = io.github.nanamochi.rosu_pp_jar.Beatmap.fromPath(beatmapPath);
    switch (score.getMode()) {
      case TAIKO -> rosuBeatmap.convert(GameMode.TAIKO, null);
      case CATCH -> rosuBeatmap.convert(GameMode.CATCH, null);
      case MANIA -> {
        Mods keyCountMod = Mods.getManiaKeyCount(Mods.fromBitmask(score.getMods()));
        rosuBeatmap.convert(
            GameMode.MANIA, io.github.nanamochi.rosu_pp_jar.Mods.fromBits(keyCountMod.getValue()));
      }
      default -> rosuBeatmap.convert(GameMode.OSU, null);
    }
    Performance performance = Performance.create(rosuBeatmap);
    performance.setMods(io.github.nanamochi.rosu_pp_jar.Mods.fromBits(score.getMods()));
    performance.setAccuracy((double) score.getAccuracy());
    performance.setNGeki(score.getNumGekis());
    performance.setNGeki(score.getNumKatus());
    performance.setN300(score.getNum300s());
    performance.setN100(score.getNum100s());
    performance.setN50(score.getNum50s());
    performance.setMisses(score.getNumMisses());
    performance.setCombo(score.getHighestCombo());
    performance.setLazer(false);

    return performance.calculate().pp();
  }

  @Override
  public boolean supports(CalculatorType type) {
    return type == CalculatorType.ROSU;
  }
}
