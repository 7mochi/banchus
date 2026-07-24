package pe.nanamochi.banchus.multiplayer.service

import org.springframework.stereotype.Service
import pe.nanamochi.banchus.multiplayer.entity.MatchGame
import pe.nanamochi.banchus.multiplayer.repository.MatchGameRepository
import pe.nanamochi.banchus.core.util.runDatabaseCatching

@Service
class MatchGameService(private val matchGameRepository: MatchGameRepository) {
    fun create(matchGame: MatchGame) = runDatabaseCatching { matchGameRepository.save(matchGame) }

    fun gameEnded(matchId: Long) = matchGameRepository.gameEnded(matchId)
}
