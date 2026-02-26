package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import jakarta.transaction.Transactional
import kotlin.math.pow
import kotlin.math.roundToLong
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Score
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.database.repository.StatRepository
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.StatNotFound
import pe.nanamochi.banchus.util.runDatabaseCatching

@Service
class StatService(private val statRepository: StatRepository) {
    companion object {
        private const val DECAY = 0.95
    }

    @Transactional
    fun createAllGamemodes(user: User): Result<List<Stat>, DomainMessage> = runDatabaseCatching {
        Mode.entries.map { mode ->
            statRepository.save(
                Stat(
                    user = user,
                    gamemode = mode,
                    rankedScore = 0L,
                    totalScore = 0L,
                    accuracy = 0.0,
                    playCount = 0,
                    performancePoints = 0,
                )
            )
        }
    }

    fun findByUserAndGamemode(user: User, gamemode: Mode): Result<Stat, StatNotFound> =
        statRepository.findByUserAndGamemode(user, gamemode).toResultOr { StatNotFound }

    fun update(stat: Stat): Result<Stat, DomainMessage> =
        if (statRepository.existsById(stat.id)) {
            runDatabaseCatching { statRepository.save(stat) }
        } else {
            Err(StatNotFound)
        }

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
