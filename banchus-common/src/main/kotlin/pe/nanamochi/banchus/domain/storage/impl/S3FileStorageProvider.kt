package pe.nanamochi.banchus.domain.storage.impl

import org.slf4j.LoggerFactory
import pe.nanamochi.banchus.domain.storage.FileStorageProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client

class S3FileStorageProvider(private val s3Client: S3Client, private val bucketName: String) :
    FileStorageProvider {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun initialize(buckets: List<String>) {}

    override fun read(bucket: String, key: String): ByteArray? =
        runCatching {
                s3Client.getObject { it.bucket(bucketName).key("$bucket/$key") }.readAllBytes()
            }
            .getOrNull()

    override fun write(bucket: String, key: String, content: ByteArray) {
        s3Client.putObject(
            { it.bucket(bucketName).key("$bucket/$key") },
            RequestBody.fromBytes(content),
        )
        log.debug("File written to S3 storage: {}/{}", bucket, key)
    }

    override fun delete(bucket: String, key: String) {
        s3Client.deleteObject { it.bucket(bucketName).key("$bucket/$key") }
    }

    override fun exists(bucket: String, key: String): Boolean =
        runCatching {
                s3Client.headObject { it.bucket(bucketName).key("$bucket/$key") }
                true
            }
            .getOrDefault(false)
}
