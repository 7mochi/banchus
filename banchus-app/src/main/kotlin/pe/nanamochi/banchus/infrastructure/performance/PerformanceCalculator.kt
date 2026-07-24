package pe.nanamochi.banchus.infrastructure.performance

import com.github.michaelbull.result.Result
import pe.nanamochi.banchus.core.error.PerformanceError
import pe.nanamochi.banchus.score.entity.Score

interface PerformanceCalculator {
    fun calculate(beatmapPath: String, score: Score): Result<Double, PerformanceError>

    fun calculate(beatmapData: ByteArray, score: Score): Result<Double, PerformanceError>

    fun supports(type: CalculatorType): Boolean
}
