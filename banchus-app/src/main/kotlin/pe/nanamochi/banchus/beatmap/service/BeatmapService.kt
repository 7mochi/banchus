package pe.nanamochi.banchus.beatmap.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.orElse
import com.github.michaelbull.result.toResultOr
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.beatmap.entity.Beatmap
import pe.nanamochi.banchus.beatmap.mapper.BeatmapMapper
import pe.nanamochi.banchus.beatmap.repository.BeatmapRepository
import pe.nanamochi.banchus.core.error.BeatmapNotFound
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.InternalError
import pe.nanamochi.banchus.core.service.StorageService
import pe.nanamochi.banchus.core.util.runDatabaseCatching
import pe.nanamochi.banchus.core.util.toMd5
import pe.nanamochi.banchus.infrastructure.client.OsuApiClient

@Service
class BeatmapService(
    private val beatmapRepository: BeatmapRepository,
    private val beatmapsetService: BeatmapsetService,
    private val beatmapMapper: BeatmapMapper,
    private val storageService: StorageService,
    private val osuApiClient: OsuApiClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun create(beatmap: Beatmap): Result<Beatmap, DomainMessage> = runDatabaseCatching {
        beatmapRepository.save(beatmap)
    }

    fun update(beatmap: Beatmap): Result<Beatmap, DomainMessage> =
        if (beatmapRepository.existsById(beatmap.id)) {
            runDatabaseCatching { beatmapRepository.save(beatmap) }
        } else {
            Err(BeatmapNotFound)
        }

    fun fetchOneById(id: Int): Result<Beatmap, BeatmapNotFound> =
        beatmapRepository.findBeatmapById(id).toResultOr { BeatmapNotFound }

    fun fetchOneByMd5(md5: String): Result<Beatmap, BeatmapNotFound> =
        beatmapRepository.findByMd5(md5).toResultOr { BeatmapNotFound }

    fun getOrCreateBeatmap(beatmapMd5: String): Result<Beatmap, DomainMessage> = binding {
        val localBeatmap =
            fetchOneByMd5(beatmapMd5).getOrElse { _ ->
                log.debug("Beatmap {} not found local. Querying osu!api...", beatmapMd5)

                val apiBeatmap =
                    osuApiClient.getBeatmap(beatmapMd5).toResultOr { BeatmapNotFound }.bind()
                val beatmapsetId = apiBeatmap.beatmapsetId.toResultOr { InternalError }.bind()

                val beatmapset =
                    beatmapsetService
                        .fetchOneById(beatmapsetId)
                        .orElse { beatmapsetService.create(apiBeatmap) }
                        .bind()

                val newBeatmap =
                    create(beatmapMapper.fromApi(apiBeatmap).apply { this.beatmapset = beatmapset })
                        .bind()
                getOrDownloadOsuFile(newBeatmap.id, beatmapMd5).bind()

                newBeatmap
            }

        log.debug("Found local beatmap for MD5 {}: {}", beatmapMd5, localBeatmap.id)

        updateBeatmapIfOutdated(localBeatmap, beatmapMd5).bind()
    }

    fun getOrDownloadOsuFile(
        beatmapId: Int,
        expectedMd5: String?,
    ): Result<ByteArray, DomainMessage> = binding {
        storageService.getBeatmap(beatmapId).onSuccess { data ->
            val isValid =
                expectedMd5?.let { md5 -> data.toMd5().equals(md5, ignoreCase = true) } ?: true
            if (isValid) return@binding data
            log.debug("Local .osu file for {} does not match MD5. Redownloading...", beatmapId)
        }

        val downloaded = osuApiClient.getOsuFile(beatmapId).toResultOr { BeatmapNotFound }.bind()
        storageService.saveBeatmap(beatmapId, downloaded).bind()
        downloaded
    }

    private fun updateBeatmapIfOutdated(
        beatmap: Beatmap,
        currentMd5: String,
    ): Result<Beatmap, DomainMessage> = binding {
        val beatmapset = beatmap.beatmapset ?: return@binding beatmap

        val osuApiBeatmaps = osuApiClient.getBeatmaps(beatmapset.id)
        if (osuApiBeatmaps.isEmpty()) return@binding beatmap

        val remoteLastUpdate =
            osuApiBeatmaps.mapNotNull { it.lastUpdate }.maxOrNull() ?: beatmapset.lastUpdated

        if (beatmapset.lastUpdated.isBefore(remoteLastUpdate)) {
            log.debug("Updating metadata for beatmapset {} (Outdated)", beatmapset.id)

            beatmapset.lastUpdated = remoteLastUpdate
            beatmapsetService.update(beatmapset).bind()

            var updatedTarget = beatmap

            for (apiMap in osuApiBeatmaps) {
                val apiMd5 = apiMap.fileMd5 ?: continue
                val apiId = apiMap.beatmapId ?: continue

                if (apiMd5.equals(currentMd5, ignoreCase = true)) {
                    getOrDownloadOsuFile(apiId, apiMd5).bind()
                }

                fetchOneByMd5(apiMd5).onSuccess { local ->
                    local.apply {
                        lastUpdated = apiMap.lastUpdate ?: remoteLastUpdate
                        starRating = apiMap.difficultyRating ?: starRating
                    }
                    update(local).bind()

                    if (apiMd5.equals(beatmap.md5, ignoreCase = true)) {
                        updatedTarget = local
                    }
                }
            }
            return@binding updatedTarget
        }
        beatmap
    }
}
