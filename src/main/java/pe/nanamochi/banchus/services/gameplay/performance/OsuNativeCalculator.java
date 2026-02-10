package pe.nanamochi.banchus.services.gameplay.performance;

import io.github.nanamochi.osu_native.wrapper.factories.DifficultyCalculatorFactory;
import io.github.nanamochi.osu_native.wrapper.factories.PerformanceCalculatorFactory;
import io.github.nanamochi.osu_native.wrapper.objects.Beatmap;
import io.github.nanamochi.osu_native.wrapper.objects.Mod;
import io.github.nanamochi.osu_native.wrapper.objects.ModsCollection;
import io.github.nanamochi.osu_native.wrapper.objects.Ruleset;
import io.github.nanamochi.osu_native.wrapper.objects.ScoreInfo;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.entities.commons.Mods;
import pe.nanamochi.banchus.entities.db.Score;

@Component
public class OsuNativeCalculator implements PerformanceCalculator {
  @Override
  public double calculate(String beatmapPath, Score score) throws Exception {
    List<Mod> createdMods = new ArrayList<>();
    try (Beatmap beatmap = Beatmap.fromFile(beatmapPath);
        Ruleset ruleset = Ruleset.fromId(score.getMode().getValue());
        var difficultyCalculator = DifficultyCalculatorFactory.create(ruleset, beatmap);
        var performanceCalculator = PerformanceCalculatorFactory.create(ruleset);
        ModsCollection modsCollection = ModsCollection.create()) {

      for (var modInfo : Mods.fromBitmask(score.getMods())) {
        Mod mod = Mod.create(modInfo.getInitial());
        createdMods.add(mod);
        modsCollection.add(mod);
      }

      Mod classicMod = Mod.create("CL");
      createdMods.add(classicMod);
      modsCollection.add(classicMod);

      ScoreInfo scoreInfo = new ScoreInfo();
      scoreInfo.setAccuracy(score.getAccuracy() / 100.0);
      scoreInfo.setMaxCombo(score.getHighestCombo());
      scoreInfo.setCountGreat(score.getNum300s());
      scoreInfo.setCountOk(score.getNum100s());
      scoreInfo.setCountMeh(score.getNum50s());
      scoreInfo.setCountMiss(score.getNumMisses());

      var difficultyAttributes = difficultyCalculator.calculate(modsCollection);
      return performanceCalculator
          .calculate(ruleset, beatmap, modsCollection, scoreInfo, difficultyAttributes)
          .getTotal();
    } finally {
      for (Mod mod : createdMods) {
        try {
          mod.close();
        } catch (Exception e) {
          // TODO: log this or handle it in some way
        }
      }
    }
  }

  @Override
  public boolean supports(CalculatorType type) {
    return type == CalculatorType.OSU_NATIVE;
  }
}
