package pe.nanamochi.banchus.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.Match

@Repository
interface MatchRepository : JpaRepository<Match, Long> {
    fun findMatchById(id: Long): Match?
}
