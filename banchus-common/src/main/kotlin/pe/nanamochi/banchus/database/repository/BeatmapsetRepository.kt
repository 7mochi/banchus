package pe.nanamochi.banchus.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.Beatmapset

@Repository
interface BeatmapsetRepository : JpaRepository<Beatmapset, Int> {
    fun findBeatmapsetById(id: Int): Beatmapset?
}
