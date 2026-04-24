package pe.nanamochi.banchus.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.MatchEvent

@Repository interface MatchEventRepository : JpaRepository<MatchEvent, Int> {}
