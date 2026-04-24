package pe.nanamochi.banchus.service

import com.github.michaelbull.result.toResultOr
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Match
import pe.nanamochi.banchus.database.repository.MatchRepository
import pe.nanamochi.banchus.domain.error.MatchNotFound
import pe.nanamochi.banchus.util.runDatabaseCatching

@Service
class MatchService(private val matchRepository: MatchRepository) {
    fun create(match: Match) = runDatabaseCatching { matchRepository.save(match) }

    fun fetchOneById(matchId: Long) =
        matchRepository.findMatchById(matchId).toResultOr { MatchNotFound }
}
