package pe.nanamochi.banchus.infrastructure.storage

import org.slf4j.LoggerFactory
import pe.nanamochi.banchus.core.enums.StorageBucket
import pe.nanamochi.banchus.core.storage.FileStorageProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client

class S3FileStorageProvider(private val s3Client: S3Client, private val bucketName: String) :
    FileStorageProvider {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun initialize(buckets: List<StorageBucket>) {}

    override fun read(bucket: StorageBucket, key: String): ByteArray? =
        runCatching {
                s3Client
                    .getObject { it.bucket(bucketName).key("${bucket.value}/$key") }
                    .readAllBytes()
            }
            .getOrNull()

    override fun write(bucket: StorageBucket, key: String, content: ByteArray) {
        s3Client.putObject(
            { it.bucket(bucketName).key("${bucket.value}/$key") },
            RequestBody.fromBytes(content),
        )
        log.debug("File written to S3 storage: {}/{}", bucket, key)
    }

    override fun delete(bucket: StorageBucket, key: String) {
        s3Client.deleteObject { it.bucket(bucketName).key("${bucket.value}/$key") }
    }

    override fun exists(bucket: StorageBucket, key: String): Boolean =
        runCatching {
                s3Client.headObject { it.bucket(bucketName).key("${bucket.value}/$key") }
                true
            }
            .getOrDefault(false)
}
