package pe.nanamochi.banchus.infrastructure.performance

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.github.nanamochi.osu_native.wrapper.factories.DifficultyCalculatorFactory
import io.github.nanamochi.osu_native.wrapper.factories.PerformanceCalculatorFactory
import io.github.nanamochi.osu_native.wrapper.objects.Beatmap
import io.github.nanamochi.osu_native.wrapper.objects.Mod
import io.github.nanamochi.osu_native.wrapper.objects.ModsCollection
import io.github.nanamochi.osu_native.wrapper.objects.Ruleset
import io.github.nanamochi.osu_native.wrapper.objects.ScoreInfo
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.score.entity.Score
import pe.nanamochi.banchus.core.enums.Mode
import pe.nanamochi.banchus.core.enums.Mods
import pe.nanamochi.banchus.core.error.CalculationFailed
import pe.nanamochi.banchus.core.error.PerformanceError

@Component
class OsuNativeCalculator : PerformanceCalculator {

    override fun calculate(beatmapPath: String, score: Score): Result<Double, PerformanceError> {
        return runCatching { Beatmap.fromFile(beatmapPath).use { it.computePerformance(score) } }
            .mapError { CalculationFailed }
    }

    override fun calculate(beatmapData: ByteArray, score: Score): Result<Double, PerformanceError> {
        return runCatching { Beatmap.fromBytes(beatmapData).use { it.computePerformance(score) } }
            .mapError { CalculationFailed }
    }

    private fun Beatmap.computePerformance(score: Score): Double {
        return Ruleset.fromId(score.mode.value.toInt()).use { ruleset ->
            val diffCalc = DifficultyCalculatorFactory.create(ruleset, this)
            val perfCalc = PerformanceCalculatorFactory.create(ruleset)

            val modNames = Mods.fromBitmask(score.mods.toUInt()).map { it.initial } + "CL"

            val mods = modNames.map { Mod.create(it) }

            mods.useEach { modsList ->
                ModsCollection.create().use { modsCollection ->
                    modsList.forEach(modsCollection::add)

                    val scoreInfo =
                        ScoreInfo().apply {
                            accuracy = score.accuracy / 100.0
                            maxCombo = score.highestCombo
                            countGreat = score.num300s
                            countOk = score.num100s
                            countMeh = score.num50s
                            countMiss = score.numMisses

                            when (score.mode) {
                                Mode.MANIA -> {
                                    countPerfect = score.numGekis
                                    countGood = score.numKatus
                                }
                                Mode.CATCH -> {
                                    countLargeTickHit = score.num100s
                                    countSmallTickHit = score.num50s
                                    countSmallTickMiss = score.numKatus
                                }
                                else -> {}
                            }
                        }

                    val diffAttrs = diffCalc.calculate(modsCollection)
                    perfCalc.calculate(ruleset, this, modsCollection, scoreInfo, diffAttrs).total
                }
            }
        }
    }

    private fun <T : AutoCloseable, R> List<T>.useEach(block: (List<T>) -> R): R {
        if (isEmpty()) return block(emptyList())

        val head = first()
        val tail = drop(1)

        return head.use { tail.useEach { processedTail -> block(listOf(head) + processedTail) } }
    }

    override fun supports(type: CalculatorType) = type == CalculatorType.OSU_NATIVE
}
