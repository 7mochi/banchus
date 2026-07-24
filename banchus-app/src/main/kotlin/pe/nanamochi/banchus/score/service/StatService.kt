package pe.nanamochi.banchus.score.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import kotlin.math.pow
import kotlin.math.roundToInt
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.identity.entity.User
import pe.nanamochi.banchus.score.entity.Score
import pe.nanamochi.banchus.score.entity.Stat
import pe.nanamochi.banchus.score.repository.StatRepository
import pe.nanamochi.banchus.core.enums.Mode
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.StatNotFound
import pe.nanamochi.banchus.core.util.runDatabaseCatching

private const val DECAY = 0.95

@Service
class StatService(private val statRepository: StatRepository) {
    fun update(stat: Stat): Result<Stat, DomainMessage> =
        if (statRepository.existsById(stat.id)) {
            runDatabaseCatching { statRepository.save(stat) }
        } else {
            Err(StatNotFound)
        }

    fun createAllModes(user: User): Result<List<Stat>, DomainMessage> = runDatabaseCatching {
        Mode.entries.map { mode -> statRepository.save(Stat(user = user, mode = mode)) }
    }

    fun fetchOne(userId: Int, mode: Mode): Result<Stat, StatNotFound> =
        statRepository.findByUserIdAndMode(userId, mode).toResultOr { StatNotFound }

    fun fetchUserStats(user: User): Result<List<Stat>, StatNotFound> =
        statRepository.findByUser(user).toResultOr { StatNotFound }

    fun calculateWeightedAccuracy(topScores: List<Score>): Double {
        if (topScores.isEmpty()) return 0.0

        val (weightedSum, totalWeight) =
            topScores.foldIndexed(0.0 to 0.0) { i, acc, score ->
                val weight = DECAY.pow(i)
                (acc.first + (score.accuracy * weight)) to (acc.second + weight)
            }

        return weightedSum / totalWeight
    }

    fun calculateWeightedPp(topScores: List<Score>, rankedScoreCount: Int = 0): Int {
        val weightedPp =
            topScores
                .asSequence()
                .withIndex()
                .sumOf { (i, score) -> score.performancePoints * DECAY.pow(i) }
                .roundToInt()

        return weightedPp + calculateBonusPp(rankedScoreCount)
    }

    fun calculateBonusPp(rankedScoreCount: Int): Int =
        (416.6667 * (1.0 - 0.9994.pow(rankedScoreCount))).roundToInt()
}
