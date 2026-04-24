package pe.nanamochi.banchus.service

import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.MatchEvent
import pe.nanamochi.banchus.database.repository.MatchEventRepository
import pe.nanamochi.banchus.util.runDatabaseCatching

@Service
class MatchEventService(private val matchEventRepository: MatchEventRepository) {
    fun create(matchEvent: MatchEvent) = runDatabaseCatching {
        matchEventRepository.save(matchEvent)
    }
}
