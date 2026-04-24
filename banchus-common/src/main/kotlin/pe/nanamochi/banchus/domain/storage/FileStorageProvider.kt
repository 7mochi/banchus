package pe.nanamochi.banchus.domain.storage

import pe.nanamochi.banchus.domain.enums.StorageBucket

interface FileStorageProvider {
    fun initialize(buckets: List<StorageBucket>)

    fun read(bucket: StorageBucket, key: String): ByteArray?

    fun write(bucket: StorageBucket, key: String, content: ByteArray)

    fun delete(bucket: StorageBucket, key: String)

    fun exists(bucket: StorageBucket, key: String): Boolean
}
