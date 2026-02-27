package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.orElse
import com.github.michaelbull.result.toResultOr
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.domain.errors.FileNotFound
import pe.nanamochi.banchus.domain.errors.StorageError
import pe.nanamochi.banchus.domain.storage.FileStorageProvider
import pe.nanamochi.banchus.util.Security
import pe.nanamochi.banchus.util.runStorageCatching

@Service
class StorageService(private val provider: FileStorageProvider) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val AVATARS = "avatars_files"
        const val BEATMAPS = "osu_beatmap_files"
        const val REPLAYS = "replays_files"
        const val SCREENSHOTS = "screenshots_files"

        private val ALL_BUCKETS = listOf(AVATARS, BEATMAPS, REPLAYS, SCREENSHOTS)
    }

    fun initStorage() {
        provider.initialize(ALL_BUCKETS)

        if (!provider.exists(AVATARS, "default.png")) {
            javaClass
                .getResourceAsStream("/images/default.png")
                ?.use { it.readAllBytes() }
                ?.let { bytes ->
                    provider.write(AVATARS, "default.png", bytes)
                    log.info("Default avatar initialized successfully.")
                } ?: log.warn("System resource /images/default.png not found!")
        }
    }

    fun getAvatar(userId: String): Result<ByteArray, FileNotFound> =
        provider
            .read(AVATARS, userId.asPng())
            .toResultOr { FileNotFound }
            .orElse { provider.read(AVATARS, "default.png").toResultOr { FileNotFound } }

    fun saveAvatar(userId: String, content: ByteArray): Result<Unit, StorageError> =
        runStorageCatching {
            provider.write(AVATARS, userId.asPng(), content)
        }

    fun getBeatmap(beatmapId: Int): Result<ByteArray, FileNotFound> =
        provider.read(BEATMAPS, beatmapId.asOsu()).toResultOr { FileNotFound }

    fun saveBeatmap(beatmapId: Int, content: ByteArray): Result<Unit, StorageError> =
        runStorageCatching {
            provider.write(BEATMAPS, beatmapId.asOsu(), content)
        }

    fun beatmapExists(beatmapId: Int): Boolean = provider.exists(BEATMAPS, beatmapId.asOsu())

    fun getReplay(scoreId: Long): Result<ByteArray, FileNotFound> =
        provider.read(REPLAYS, scoreId.asOsr()).toResultOr { FileNotFound }

    fun saveReplay(scoreId: Long, content: ByteArray): Result<Unit, StorageError> =
        runStorageCatching {
            provider.write(REPLAYS, scoreId.asOsr(), content)
        }

    fun getScreenshot(screenshotId: String): Result<ByteArray, FileNotFound> =
        provider.read(SCREENSHOTS, screenshotId.asPng()).toResultOr { FileNotFound }

    fun saveScreenshot(content: ByteArray): Result<String, StorageError> = runStorageCatching {
        Security.generateToken(6).let { id ->
            provider.write(SCREENSHOTS, id.asPng(), content)
            id
        }
    }

    private fun Any.asPng() = "$this.png"

    private fun Any.asOsu() = "$this.osu"

    private fun Any.asOsr() = "$this.osr"
}
