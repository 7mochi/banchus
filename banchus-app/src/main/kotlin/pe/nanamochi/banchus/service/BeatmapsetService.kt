package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Beatmapset
import pe.nanamochi.banchus.database.repository.BeatmapsetRepository
import pe.nanamochi.banchus.domain.errors.BeatmapsetNotFound
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.dto.external.OsuApiBeatmap
import pe.nanamochi.banchus.mapper.BeatmapsetMapper
import pe.nanamochi.banchus.util.runDatabaseCatching

@Service
class BeatmapsetService(
    private val beatmapsetRepository: BeatmapsetRepository,
    private val beatmapsetMapper: BeatmapsetMapper,
) {
    fun findById(id: Int): Result<Beatmapset, BeatmapsetNotFound> =
        beatmapsetRepository.findBeatmapsetById(id).toResultOr { BeatmapsetNotFound }

    fun create(beatmapset: Beatmapset): Result<Beatmapset, DomainMessage> = runDatabaseCatching {
        beatmapsetRepository.save(beatmapset)
    }

    fun create(apiBeatmap: OsuApiBeatmap): Result<Beatmapset, DomainMessage> =
        create(beatmapsetMapper.fromApi(apiBeatmap))

    fun update(beatmapset: Beatmapset): Result<Beatmapset, DomainMessage> =
        if (beatmapsetRepository.existsById(beatmapset.id)) {
            runDatabaseCatching { beatmapsetRepository.save(beatmapset) }
        } else {
            Err(BeatmapsetNotFound)
        }
}
