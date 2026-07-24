package pe.nanamochi.banchus.core.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.orElse
import com.github.michaelbull.result.toResultOr
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.core.enums.StorageBucket
import pe.nanamochi.banchus.core.error.FileNotFound
import pe.nanamochi.banchus.core.error.StorageError
import pe.nanamochi.banchus.core.storage.FileStorageProvider
import pe.nanamochi.banchus.core.util.Security
import pe.nanamochi.banchus.core.util.runStorageCatching

@Service
class StorageService(private val provider: FileStorageProvider) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun initStorage() {
        provider.initialize(StorageBucket.entries.map { it })

        if (!provider.exists(StorageBucket.AVATARS, "default.png")) {
            javaClass
                .getResourceAsStream("/images/default.png")
                ?.use { it.readAllBytes() }
                ?.let { bytes ->
                    provider.write(StorageBucket.AVATARS, "default.png", bytes)
                    log.info("Default avatar initialized successfully.")
                } ?: log.warn("System resource /images/default.png not found!")
        }
    }

    fun getAvatar(userId: String): Result<ByteArray, FileNotFound> =
        provider
            .read(StorageBucket.AVATARS, userId.asPng())
            .toResultOr { FileNotFound }
            .orElse {
                provider.read(StorageBucket.AVATARS, "default.png").toResultOr { FileNotFound }
            }

    fun saveAvatar(userId: String, content: ByteArray): Result<Unit, StorageError> =
        runStorageCatching {
            provider.write(StorageBucket.AVATARS, userId.asPng(), content)
        }

    fun getBeatmap(beatmapId: Int): Result<ByteArray, FileNotFound> =
        provider.read(StorageBucket.BEATMAPS, beatmapId.asOsu()).toResultOr { FileNotFound }

    fun saveBeatmap(beatmapId: Int, content: ByteArray): Result<Unit, StorageError> =
        runStorageCatching {
            provider.write(StorageBucket.BEATMAPS, beatmapId.asOsu(), content)
        }

    fun beatmapExists(beatmapId: Int): Boolean =
        provider.exists(StorageBucket.BEATMAPS, beatmapId.asOsu())

    fun getReplay(scoreId: Long): Result<ByteArray, FileNotFound> =
        provider.read(StorageBucket.REPLAYS, scoreId.asOsr()).toResultOr { FileNotFound }

    fun saveReplay(scoreId: Long, content: ByteArray): Result<Unit, StorageError> =
        runStorageCatching {
            provider.write(StorageBucket.REPLAYS, scoreId.asOsr(), content)
        }

    fun getScreenshot(screenshotId: String): Result<ByteArray, FileNotFound> =
        provider.read(StorageBucket.SCREENSHOTS, screenshotId.asPng()).toResultOr { FileNotFound }

    fun saveScreenshot(content: ByteArray): Result<String, StorageError> = runStorageCatching {
        Security.generateToken(6).let { id ->
            provider.write(StorageBucket.SCREENSHOTS, id.asPng(), content)
            id
        }
    }

    private fun Any.asPng() = "$this.png"

    private fun Any.asOsu() = "$this.osu"

    private fun Any.asOsr() = "$this.osr"
}
