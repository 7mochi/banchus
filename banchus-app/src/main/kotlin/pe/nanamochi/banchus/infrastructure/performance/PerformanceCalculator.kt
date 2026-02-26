package pe.nanamochi.banchus.infrastructure.performance

import com.github.michaelbull.result.Result
import pe.nanamochi.banchus.database.entity.Score
import pe.nanamochi.banchus.domain.errors.PerformanceError

interface PerformanceCalculator {
    fun calculate(beatmapPath: String, score: Score): Result<Double, PerformanceError>

    fun calculate(beatmapData: ByteArray, score: Score): Result<Double, PerformanceError>

    fun supports(type: CalculatorType): Boolean
}
