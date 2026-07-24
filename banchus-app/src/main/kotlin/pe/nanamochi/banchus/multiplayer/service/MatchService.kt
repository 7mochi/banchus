package pe.nanamochi.banchus.multiplayer.service

import com.github.michaelbull.result.toResultOr
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.multiplayer.entity.Match
import pe.nanamochi.banchus.multiplayer.repository.MatchRepository
import pe.nanamochi.banchus.core.error.MatchNotFound
import pe.nanamochi.banchus.core.util.runDatabaseCatching

@Service
class MatchService(private val matchRepository: MatchRepository) {
    fun create(match: Match) = runDatabaseCatching { matchRepository.save(match) }

    fun fetchOneById(matchId: Long) =
        matchRepository.findMatchById(matchId).toResultOr { MatchNotFound }
}
