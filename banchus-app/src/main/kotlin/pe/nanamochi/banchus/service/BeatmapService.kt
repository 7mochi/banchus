package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.orElse
import com.github.michaelbull.result.toResultOr
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Beatmap
import pe.nanamochi.banchus.database.entity.Beatmapset
import pe.nanamochi.banchus.database.repository.BeatmapRepository
import pe.nanamochi.banchus.domain.errors.BeatmapNotFound
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.InternalError
import pe.nanamochi.banchus.dto.external.OsuApiBeatmap
import pe.nanamochi.banchus.infrastructure.clients.OsuApiClient
import pe.nanamochi.banchus.mapper.BeatmapMapper
import pe.nanamochi.banchus.util.runDatabaseCatching
import pe.nanamochi.banchus.util.toMd5

@Service
class BeatmapService(
    private val beatmapRepository: BeatmapRepository,
    private val beatmapsetService: BeatmapsetService,
    private val beatmapMapper: BeatmapMapper,
    private val storageService: StorageService,
    private val osuApiClient: OsuApiClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun findById(id: Int): Result<Beatmap, BeatmapNotFound> =
        beatmapRepository.findBeatmapById(id).toResultOr { BeatmapNotFound }

    fun findByMd5(md5: String): Result<Beatmap, BeatmapNotFound> =
        beatmapRepository.findByMd5(md5).toResultOr { BeatmapNotFound }

    fun create(beatmap: Beatmap): Result<Beatmap, DomainMessage> = runDatabaseCatching {
        beatmapRepository.save(beatmap)
    }

    fun create(apiBeatmap: OsuApiBeatmap, beatmapset: Beatmapset): Result<Beatmap, DomainMessage> {
        val beatmap = beatmapMapper.fromApi(apiBeatmap).apply { this.beatmapset = beatmapset }
        return create(beatmap)
    }

    fun update(beatmap: Beatmap): Result<Beatmap, DomainMessage> =
        if (beatmapRepository.existsById(beatmap.id)) {
            runDatabaseCatching { beatmapRepository.save(beatmap) }
        } else {
            Err(BeatmapNotFound)
        }

    @Transactional
    fun incrementStats(id: Int, passed: Boolean): Result<Unit, DomainMessage> =
        runDatabaseCatching { beatmapRepository.incrementStats(id, passed) }.map {}

    fun getOrCreateBeatmap(beatmapMd5: String): Result<Beatmap, DomainMessage> = binding {
        val localBeatmap =
            findByMd5(beatmapMd5).getOrElse { _ ->
                log.debug("Beatmap {} not found local. Querying osu!api...", beatmapMd5)

                val apiData =
                    osuApiClient.getBeatmap(beatmapMd5).toResultOr { BeatmapNotFound }.bind()

                val setId = apiData.beatmapsetId.toResultOr { BeatmapNotFound }.bind()

                val beatmapset =
                    beatmapsetService
                        .findById(setId)
                        .orElse { beatmapsetService.create(apiData) }
                        .bind()

                osuApiClient
                    .getBeatmaps(setId)
                    .map { apiMap ->
                        val currentMd5 = apiMap.fileMd5.toResultOr { InternalError }.bind()

                        val current =
                            findByMd5(currentMd5).getOrElse { _ ->
                                val newMap = create(apiMap, beatmapset).bind()
                                val bId = apiMap.beatmapId.toResultOr { InternalError }.bind()
                                getOrDownloadOsuFile(bId, currentMd5).bind()
                                newMap
                            }
                        current
                    }
                    .find { it.md5.equals(beatmapMd5, ignoreCase = true) }
                    ?: Err(BeatmapNotFound).bind()
            }

        log.debug("Found local beatmap for MD5 {}: {}", beatmapMd5, localBeatmap.id)

        updateBeatmapIfOutdated(localBeatmap).bind()
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

    private fun updateBeatmapIfOutdated(beatmap: Beatmap): Result<Beatmap, DomainMessage> =
        binding {
            val beatmapset = beatmap.beatmapset ?: return@binding beatmap

            val osuApiBeatmaps = osuApiClient.getBeatmaps(beatmapset.id)
            if (osuApiBeatmaps.isEmpty()) return@binding beatmap

            val remoteLastUpdate =
                osuApiBeatmaps.mapNotNull { it.lastUpdate }.maxOrNull() ?: beatmapset.lastUpdated

            if (beatmapset.lastUpdated.isBefore(remoteLastUpdate)) {
                log.debug("Updating beatmapset {} (Outdated)", beatmapset.id)

                beatmapset.lastUpdated = remoteLastUpdate
                beatmapsetService.update(beatmapset).bind()

                var updatedTarget = beatmap

                for (apiMap in osuApiBeatmaps) {
                    val apiMd5 = apiMap.fileMd5 ?: continue
                    val apiId = apiMap.beatmapId ?: continue

                    getOrDownloadOsuFile(apiId, apiMd5)

                    findByMd5(apiMd5).onSuccess { local ->
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
