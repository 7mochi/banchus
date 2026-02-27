package pe.nanamochi.banchus.database.repository

import io.lettuce.core.dynamic.annotation.Param
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.Beatmap

@Repository
interface BeatmapRepository : JpaRepository<Beatmap, Int> {
    fun findBeatmapById(id: Int): Beatmap?

    fun findByMd5(md5: String): Beatmap?

    @Modifying
    @Query(
        value =
            "UPDATE beatmaps SET playcount = playcount + 1, passcount = passcount + :passed WHERE id = :id",
        nativeQuery = true,
    )
    fun incrementStats(@Param("id") id: Int, @Param("passed") passed: Boolean)
}
