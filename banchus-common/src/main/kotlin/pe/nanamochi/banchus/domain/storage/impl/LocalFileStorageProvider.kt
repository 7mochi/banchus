package pe.nanamochi.banchus.domain.storage.impl

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import pe.nanamochi.banchus.domain.storage.FileStorageProvider

class LocalFileStorageProvider(private val basePath: String = ".data/") : FileStorageProvider {
    override fun initialize(buckets: List<String>) {
        buckets.forEach { bucket ->
            Path.of(basePath, bucket).takeIf { !it.exists() }?.createDirectories()
        }
    }

    override fun read(bucket: String, key: String): ByteArray? =
        runCatching { Path.of(basePath, bucket, key).readBytes() }.getOrNull()

    override fun write(bucket: String, key: String, content: ByteArray) {
        Path.of(basePath, bucket, key).also { it.parent.createDirectories() }.writeBytes(content)
    }

    override fun delete(bucket: String, key: String) {
        runCatching { Files.deleteIfExists(Path.of(basePath, bucket, key)) }
    }

    override fun exists(bucket: String, key: String): Boolean =
        Path.of(basePath, bucket, key).exists()
}
