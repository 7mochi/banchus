package pe.nanamochi.banchus.domain.storage.impl

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import org.slf4j.LoggerFactory
import pe.nanamochi.banchus.domain.enums.StorageBucket
import pe.nanamochi.banchus.domain.storage.FileStorageProvider

class LocalFileStorageProvider(private val basePath: String = ".data/") : FileStorageProvider {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun initialize(buckets: List<StorageBucket>) {
        buckets.forEach { bucket ->
            Path.of(basePath, bucket.value).takeIf { !it.exists() }?.createDirectories()
        }
    }

    override fun read(bucket: StorageBucket, key: String): ByteArray? =
        runCatching { Path.of(basePath, bucket.value, key).readBytes() }.getOrNull()

    override fun write(bucket: StorageBucket, key: String, content: ByteArray) {
        Path.of(basePath, bucket.value, key)
            .also { it.parent.createDirectories() }
            .writeBytes(content)
        log.debug("File written to local storage: {}/{}", bucket, key)
    }

    override fun delete(bucket: StorageBucket, key: String) {
        runCatching { Files.deleteIfExists(Path.of(basePath, bucket.value, key)) }
    }

    override fun exists(bucket: StorageBucket, key: String): Boolean =
        Path.of(basePath, bucket.value, key).exists()
}
