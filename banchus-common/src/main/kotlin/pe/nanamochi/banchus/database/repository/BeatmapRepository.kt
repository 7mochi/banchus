package pe.nanamochi.banchus.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.Beatmap

@Repository
interface BeatmapRepository : JpaRepository<Beatmap, Int> {
    fun findBeatmapById(id: Int): Beatmap?

    fun findByMd5(md5: String): Beatmap?
}
