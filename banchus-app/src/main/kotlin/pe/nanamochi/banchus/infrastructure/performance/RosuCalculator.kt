package pe.nanamochi.banchus.infrastructure.performance

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.github.nanamochi.rosu_pp_jar.Beatmap
import io.github.nanamochi.rosu_pp_jar.GameMode
import io.github.nanamochi.rosu_pp_jar.Performance
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.score.entity.Score
import pe.nanamochi.banchus.core.enums.Mods
import pe.nanamochi.banchus.core.error.CalculationFailed
import pe.nanamochi.banchus.core.error.PerformanceError

@Component
class RosuCalculator : PerformanceCalculator {

    override fun calculate(beatmapPath: String, score: Score): Result<Double, PerformanceError> {
        return runCatching {
                Beatmap.fromPath(beatmapPath).use { rosuBeatmap ->
                    val mode = score.mode.toRosuMode()

                    val maniaMods =
                        if (mode == GameMode.MANIA) {
                            val keyCount =
                                Mods.getManiaKeyCount(Mods.fromBitmask(score.mods.toUInt()))
                            io.github.nanamochi.rosu_pp_jar.Mods.fromBits(keyCount.value.toInt())
                        } else null

                    rosuBeatmap.convert(mode, maniaMods)

                    Performance.create(rosuBeatmap).use { perf ->
                        perf
                            .apply {
                                setMods(io.github.nanamochi.rosu_pp_jar.Mods.fromBits(score.mods))
                                setAccuracy(score.accuracy)
                                setN300(score.num300s)
                                setN100(score.num100s)
                                setN50(score.num50s)
                                setNGeki(score.numGekis)
                                setNKatu(score.numKatus)
                                setMisses(score.numMisses)
                                setCombo(score.highestCombo)
                                setLazer(false)
                            }
                            .calculate()
                            .pp()
                    }
                }
            }
            .mapError { CalculationFailed }
    }

    override fun calculate(beatmapData: ByteArray, score: Score): Result<Double, PerformanceError> {
        return Err(CalculationFailed)
    }

    private fun pe.nanamochi.banchus.core.enums.Mode.toRosuMode(): GameMode =
        when (this) {
            pe.nanamochi.banchus.core.enums.Mode.TAIKO -> GameMode.TAIKO
            pe.nanamochi.banchus.core.enums.Mode.CATCH -> GameMode.CATCH
            pe.nanamochi.banchus.core.enums.Mode.MANIA -> GameMode.MANIA
            else -> GameMode.OSU
        }

    override fun supports(type: CalculatorType) = type == CalculatorType.ROSU
}
