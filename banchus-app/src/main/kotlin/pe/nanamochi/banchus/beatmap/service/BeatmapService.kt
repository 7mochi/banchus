package pe.nanamochi.banchus.beatmap.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.orElse
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.beatmap.entity.Beatmap
import pe.nanamochi.banchus.beatmap.enums.BeatmapRankedStatus
import pe.nanamochi.banchus.beatmap.mapper.BeatmapMapper
import pe.nanamochi.banchus.beatmap.repository.BeatmapRepository
import pe.nanamochi.banchus.core.enums.Mode
import pe.nanamochi.banchus.core.error.BeatmapNotFound
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.InternalError
import pe.nanamochi.banchus.core.error.OsuApiNotFound
import pe.nanamochi.banchus.core.error.OsuApiUnavailable
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

    fun fetchOrCreateBeatmap(beatmapMd5: String): Result<Beatmap, DomainMessage> = binding {
        val localBeatmap =
            fetchOneByMd5(beatmapMd5).getOrElse { _ ->
                log.debug("Beatmap {} not found local. Querying osu!api...", beatmapMd5)

                val apiBeatmap =
                    runCatching { osuApiClient.fetchBeatmapByMd5(beatmapMd5) }
                        .mapError { OsuApiUnavailable }
                        .bind()
                        .toResultOr { OsuApiNotFound }
                        .bind()
                val beatmapsetId = apiBeatmap.beatmapsetId.toResultOr { InternalError }.bind()

                val beatmapset =
                    beatmapsetService
                        .fetchOneById(beatmapsetId)
                        .orElse { beatmapsetService.create(apiBeatmap) }
                        .bind()

                create(
                        beatmapMapper.buildFromApi(apiBeatmap).apply {
                            this.beatmapset = beatmapset
                        }
                    )
                    .bind()
            }

        applyBeatmapRefreshIfOutdated(localBeatmap).bind()
    }

    fun fetchOrDownloadOsuFile(
        beatmapId: Int,
        expectedMd5: String?,
    ): Result<ByteArray, DomainMessage> = binding {
        val cached = storageService.fetchBeatmap(beatmapId)
        var staleData: ByteArray? = null

        cached.onSuccess { data ->
            staleData = data
            val isValid =
                expectedMd5?.let { md5 -> data.toMd5().equals(md5, ignoreCase = true) } ?: true
            if (isValid) return@binding data
            log.debug("Local .osu file for {} does not match MD5. Redownloading...", beatmapId)
        }

        val downloaded =
            runCatching { osuApiClient.fetchOsuFile(beatmapId) }
                .mapError { OsuApiUnavailable }
                .bind()
                .toResultOr { OsuApiNotFound }
                .bind()

        if (!downloaded.isValidOsuFile()) {
            log.warn("Downloaded .osu file for {} is invalid, serving stale cache", beatmapId)
            staleData?.let {
                return@binding it
            }
            Err(OsuApiUnavailable).bind()
        }

        storageService.persistBeatmap(beatmapId, downloaded).bind()

        downloaded
    }

    private fun applyBeatmapRefreshIfOutdated(beatmap: Beatmap): Result<Beatmap, DomainMessage> =
        binding {
            if (!beatmap.deservesUpdate()) return@binding beatmap

            runCatching { osuApiClient.fetchBeatmapById(beatmap.id) }
                .mapError { OsuApiUnavailable }
                .getOrElse {
                    log.warn("Failed to refresh beatmap {}", beatmap.id)
                    return@binding beatmap
                }
                ?.let { apiBeatmap ->
                    apiBeatmap.fileMd5
                        ?.takeIf { !it.equals(beatmap.md5, ignoreCase = true) }
                        ?.let { newMd5 ->
                            log.debug(
                                "MD5 changed for beatmap {} ({} -> {}), invalidating .osu cache",
                                beatmap.id,
                                beatmap.md5,
                                newMd5,
                            )
                            storageService.deleteBeatmap(beatmap.id)
                        }

                    beatmap.apply {
                        lastUpdated = apiBeatmap.lastUpdate ?: lastUpdated
                        starRating = apiBeatmap.difficultyRating ?: starRating
                        cs = apiBeatmap.diffSize ?: cs
                        ar = apiBeatmap.diffApproach ?: ar
                        od = apiBeatmap.diffOverall ?: od
                        hp = apiBeatmap.diffDrain ?: hp
                        bpm = apiBeatmap.bpm
                        maxCombo = apiBeatmap.maxCombo ?: maxCombo
                        submissionDate = apiBeatmap.submitDate ?: submissionDate
                        status =
                            apiBeatmap.approved?.let { BeatmapRankedStatus.fromValue(it) } ?: status
                        mode = apiBeatmap.mode?.let { Mode.fromValue(it) } ?: mode
                        version = apiBeatmap.version ?: version
                        playcount = apiBeatmap.playcount?.toLong() ?: playcount
                        passcount = apiBeatmap.passcount?.toLong() ?: passcount
                        totalLength = apiBeatmap.totalLength ?: totalLength
                        drainLength = apiBeatmap.hitLength ?: drainLength
                        countNormal = apiBeatmap.countNormal ?: countNormal
                        countSlider = apiBeatmap.countSlider ?: countSlider
                        countSpinner = apiBeatmap.countSpinner ?: countSpinner
                    }
                    update(beatmap).bind()
                }
                ?: run {
                    log.info(
                        "Beatmap {} returned 404 from osu! API, deleting cached .osu file",
                        beatmap.id,
                    )
                    storageService.deleteBeatmap(beatmap.id)
                    return@binding beatmap
                }
        }

    private fun ByteArray.isValidOsuFile(): Boolean {
        if (isEmpty()) return false
        val header =
            decodeToString(endIndex = minOf(size, 8192), throwOnInvalidSequence = false)
                .trimStart('\uFEFF')
        val firstLine = header.substringBefore('\n')
        return firstLine.startsWith("osu file format v") &&
            "[General]" in header &&
            "[Metadata]" in header
    }
}
