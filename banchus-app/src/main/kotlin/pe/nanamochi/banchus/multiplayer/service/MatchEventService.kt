package pe.nanamochi.banchus.multiplayer.service

import org.springframework.stereotype.Service
import pe.nanamochi.banchus.core.util.runDatabaseCatching
import pe.nanamochi.banchus.multiplayer.entity.MatchEvent
import pe.nanamochi.banchus.multiplayer.repository.MatchEventRepository

@Service
class MatchEventService(private val matchEventRepository: MatchEventRepository) {
    fun create(matchEvent: MatchEvent) = runDatabaseCatching {
        matchEventRepository.save(matchEvent)
    }
}
