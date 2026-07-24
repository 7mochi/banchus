package pe.nanamochi.banchus.multiplayer.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.multiplayer.entity.Match

@Repository
interface MatchRepository : JpaRepository<Match, Long> {
    fun findMatchById(id: Long): Match?
}
