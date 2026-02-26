package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Score
import pe.nanamochi.banchus.domain.errors.CalculatorNotFound
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.infrastructure.performance.CalculatorType
import pe.nanamochi.banchus.infrastructure.performance.PerformanceCalculator

@Service
class PerformanceService(
    private val calculators: List<PerformanceCalculator>,
    @Value($$"${banchus.pp-calculator-type:rosu-pp}") private val defaultCalculatorAlias: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val defaultCalculatorType: CalculatorType by lazy {
        kotlin
            .runCatching { CalculatorType.fromAlias(defaultCalculatorAlias) }
            .getOrDefault(CalculatorType.OSU_NATIVE)
    }

    fun calculate(beatmapPath: String, score: Score): Result<Double, DomainMessage> =
        calculate(beatmapPath, score, defaultCalculatorType)

    fun calculate(beatmapData: ByteArray, score: Score): Result<Double, DomainMessage> =
        calculate(beatmapData, score, defaultCalculatorType)

    fun calculate(
        beatmapPath: String,
        score: Score,
        type: CalculatorType,
    ): Result<Double, DomainMessage> =
        withCalculator(score, type, "path") { it.calculate(beatmapPath, score) }

    fun calculate(
        beatmapData: ByteArray,
        score: Score,
        type: CalculatorType,
    ): Result<Double, DomainMessage> =
        withCalculator(score, type, "bytes") { it.calculate(beatmapData, score) }

    private fun <T> withCalculator(
        score: Score,
        type: CalculatorType,
        source: String,
        block: (PerformanceCalculator) -> Result<T, DomainMessage>,
    ): Result<T, DomainMessage> {
        log.debug("Calculating PP for score {} from {} using: {}", score.id, source, type)

        val calculator = calculators.find { it.supports(type) } ?: return Err(CalculatorNotFound)
        return block(calculator)
    }
}
