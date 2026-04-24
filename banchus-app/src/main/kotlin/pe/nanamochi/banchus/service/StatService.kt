package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.database.repository.StatRepository
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.domain.error.StatNotFound
import pe.nanamochi.banchus.util.runDatabaseCatching

@Service
class StatService(private val statRepository: StatRepository) {
    fun createAllModes(user: User): Result<List<Stat>, DomainMessage> = runDatabaseCatching {
        Mode.entries.map { mode -> statRepository.save(Stat(user = user, mode = mode)) }
    }

    fun fetchOne(userId: Int, mode: Mode): Result<Stat, StatNotFound> =
        statRepository.findByUserIdAndMode(userId, mode).toResultOr { StatNotFound }

    fun fetchUserStats(user: User): Result<List<Stat>, StatNotFound> =
        statRepository.findByUser(user).toResultOr {
            StatNotFound
        } // TODO: is StatNotFound correct?
}
