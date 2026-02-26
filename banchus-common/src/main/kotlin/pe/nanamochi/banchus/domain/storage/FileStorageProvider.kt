package pe.nanamochi.banchus.domain.storage

interface FileStorageProvider {
    fun initialize(buckets: List<String>)

    fun read(bucket: String, key: String): ByteArray?

    fun write(bucket: String, key: String, content: ByteArray)

    fun delete(bucket: String, key: String)

    fun exists(bucket: String, key: String): Boolean
}
