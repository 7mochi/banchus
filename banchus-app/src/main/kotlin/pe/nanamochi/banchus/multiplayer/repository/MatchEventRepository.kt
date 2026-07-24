package pe.nanamochi.banchus.multiplayer.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.multiplayer.entity.MatchEvent

@Repository interface MatchEventRepository : JpaRepository<MatchEvent, Int> {}
