package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import kotlin.math.pow
import kotlin.math.roundToLong
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Score
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.database.repository.StatRepository
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.domain.error.StatNotFound
import pe.nanamochi.banchus.util.runDatabaseCatching

private const val DECAY = 0.95

@Service
class StatService(private val statRepository: StatRepository) {
    fun create(stat: Stat): Result<Stat, DomainMessage> = runDatabaseCatching {
        statRepository.save(stat)
    }

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
        statRepository.findByUser(user).toResultOr {
            StatNotFound
        } // TODO: is StatNotFound correct?

    fun calculateWeightedAccuracy(topScores: List<Score>): Double {
        if (topScores.isEmpty()) return 0.0

        val (weightedSum, totalWeight) =
            topScores.foldIndexed(0.0 to 0.0) { i, acc, score ->
                val weight = DECAY.pow(i)
                (acc.first + (score.accuracy * weight)) to (acc.second + weight)
            }

        return weightedSum / totalWeight
    }

    fun calculateWeightedPp(topScores: List<Score>): Int =
        topScores
            .asSequence()
            .withIndex()
            .sumOf { (i, score) -> score.performancePoints * DECAY.pow(i) }
            .roundToLong()
            .toInt()
}
