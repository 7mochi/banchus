package pe.nanamochi.banchus.service

import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.MatchGame
import pe.nanamochi.banchus.database.repository.MatchGameRepository
import pe.nanamochi.banchus.util.runDatabaseCatching

@Service
class MatchGameService(private val matchGameRepository: MatchGameRepository) {
    fun create(matchGame: MatchGame) = runDatabaseCatching { matchGameRepository.save(matchGame) }

    fun gameEnded(matchId: Long) = matchGameRepository.gameEnded(matchId)
}
