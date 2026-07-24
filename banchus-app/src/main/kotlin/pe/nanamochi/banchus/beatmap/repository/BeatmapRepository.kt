package pe.nanamochi.banchus.beatmap.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.beatmap.entity.Beatmap

@Repository
interface BeatmapRepository : JpaRepository<Beatmap, Int> {
    fun findBeatmapById(id: Int): Beatmap?

    fun findByMd5(md5: String): Beatmap?
}
