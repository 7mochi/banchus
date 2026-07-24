package pe.nanamochi.banchus.multiplayer.repository

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.multiplayer.entity.MatchGame

@Repository
interface MatchGameRepository : JpaRepository<MatchGame, Int> {
    @Modifying
    @Transactional
    @Query(
        "UPDATE MatchGame mg SET mg.endTime = CURRENT_TIMESTAMP WHERE mg.match.id = :matchId AND mg.endTime IS NULL"
    )
    fun gameEnded(matchId: Long): Int
}
