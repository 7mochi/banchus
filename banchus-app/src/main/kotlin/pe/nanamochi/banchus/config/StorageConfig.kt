package pe.nanamochi.banchus.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pe.nanamochi.banchus.domain.storage.FileStorageProvider
import pe.nanamochi.banchus.domain.storage.impl.LocalFileStorageProvider
import pe.nanamochi.banchus.domain.storage.impl.S3FileStorageProvider
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class StorageConfig {
    @Bean
    @ConditionalOnProperty(
        name = ["banchus.storage.type"],
        havingValue = "local",
        matchIfMissing = true,
    )
    fun localFileStorage(): FileStorageProvider = LocalFileStorageProvider(".data/")

    @Bean
    @ConditionalOnProperty(name = ["banchus.storage.type"], havingValue = "s3")
    fun s3FileStorage(
        s3Client: S3Client,
        @Value($$"${banchus.storage.s3.bucket}") bucket: String,
    ): FileStorageProvider = S3FileStorageProvider(s3Client, bucket)
}
