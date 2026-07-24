package pe.nanamochi.banchus.infrastructure.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pe.nanamochi.banchus.core.storage.FileStorageProvider
import pe.nanamochi.banchus.infrastructure.storage.LocalFileStorageProvider
import pe.nanamochi.banchus.infrastructure.storage.S3FileStorageProvider
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class StorageConfig(private val properties: BanchusProperties) {
    @Bean
    @ConditionalOnProperty(
        name = ["banchus.storage.type"],
        havingValue = "local",
        matchIfMissing = true,
    )
    fun localFileStorage(): FileStorageProvider = LocalFileStorageProvider(".data/")

    @Bean
    @ConditionalOnProperty(name = ["banchus.storage.type"], havingValue = "s3")
    fun s3FileStorage(s3Client: S3Client): FileStorageProvider =
        S3FileStorageProvider(s3Client, properties.storage.s3.bucket)
}
