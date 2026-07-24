package pe.nanamochi.banchus.core.repository

import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.Limit
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.connection.stream.StreamReadOptions
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.core.MessageInfo

@Repository
class StreamRepository(
    private val byteArrayRedisTemplate: RedisTemplate<String, ByteArray>,
    private val messageInfoSerializer: RedisSerializer<MessageInfo>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun xadd(streamKey: String, data: ByteArray, info: MessageInfo): RecordId? {
        val infoBytes = messageInfoSerializer.serialize(info)
        val record =
            StreamRecords.newRecord()
                .ofMap(mapOf("data" to data, "info" to infoBytes))
                .withStreamKey(streamKey)

        return byteArrayRedisTemplate.opsForStream<String, ByteArray>().add(record)
    }

    fun join(sessionId: UUID, streamKey: String) {
        val latestId = getLatestMessageId(streamKey) ?: "0-0"
        byteArrayRedisTemplate
            .opsForHash<String, ByteArray>()
            .put(makeKey(sessionId), streamKey, latestId.toByteArray())
    }

    fun leave(sessionId: UUID, streamKey: String) {
        byteArrayRedisTemplate.opsForHash<String, ByteArray>().delete(makeKey(sessionId), streamKey)
    }

    fun leaveAll(sessionId: UUID) {
        byteArrayRedisTemplate.delete(makeKey(sessionId))
    }

    fun isJoined(sessionId: UUID, streamKey: String): Boolean {
        return byteArrayRedisTemplate
            .opsForHash<String, ByteArray>()
            .hasKey(makeKey(sessionId), streamKey)
    }

    fun readPendingMessages(sessionId: UUID): List<PendingMessage> {
        val offsetsKey = makeKey(sessionId)
        val offsets = byteArrayRedisTemplate.opsForHash<String, ByteArray>().entries(offsetsKey)

        if (offsets.isEmpty()) return emptyList()

        val allMessages = mutableListOf<PendingMessage>()
        val updatedOffsets = offsets.toMutableMap()
        val streamOps = byteArrayRedisTemplate.opsForStream<String, ByteArray>()

        for ((streamKey, lastIdBytes) in offsets) {
            val lastId = String(lastIdBytes, Charsets.UTF_8)

            val messages =
                runCatching {
                        streamOps.read(
                            StreamReadOptions.empty().count(100),
                            org.springframework.data.redis.connection.stream.StreamOffset.create(
                                streamKey,
                                org.springframework.data.redis.connection.stream.ReadOffset.from(
                                    lastId
                                ),
                            ),
                        )
                    }
                    .getOrNull() ?: continue

            for (message in messages) {
                val data = message.value["data"] ?: continue
                val infoBytes = message.value["info"] ?: continue
                val info =
                    runCatching { messageInfoSerializer.deserialize(infoBytes) }
                        .onFailure { log.warn("Failed to deserialize MessageInfo: ${it.message}") }
                        .getOrDefault(MessageInfo()) ?: MessageInfo()

                allMessages.add(PendingMessage(data = data, info = info))
            }

            messages.lastOrNull()?.id?.let { updatedOffsets[streamKey] = it.value.toByteArray() }
        }

        if (updatedOffsets.isNotEmpty()) {
            byteArrayRedisTemplate
                .opsForHash<String, ByteArray>()
                .putAll(offsetsKey, updatedOffsets)
        }

        return allMessages
    }

    fun clearStream(streamKey: String) {
        byteArrayRedisTemplate.delete(streamKey)
    }

    fun getLatestMessageId(streamKey: String): String? {
        val messages =
            byteArrayRedisTemplate
                .opsForStream<String, ByteArray>()
                .reverseRange(streamKey, Range.closed("+", "-"), Limit.limit().count(1))
                ?: return null
        return messages.lastOrNull()?.id?.value ?: "0-0"
    }

    fun fetchAll(): List<String> {
        val keys = mutableListOf<String>()
        val scanCursor =
            byteArrayRedisTemplate.scan(
                org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match("$BASE_KEY:*")
                    .count(100)
                    .build()
            )
        while (scanCursor.hasNext()) {
            keys.add(scanCursor.next())
        }
        return keys
    }

    fun trimMessages(streamKey: String, minId: Long): Long {
        return runCatching {
                byteArrayRedisTemplate.opsForStream<String, ByteArray>().trim(streamKey, minId)
            }
            .onFailure { log.warn("Failed to trim stream $streamKey: ${it.message}") }
            .getOrDefault(0L)
    }

    data class PendingMessage(val data: ByteArray, val info: MessageInfo)

    private fun makeKey(sessionId: UUID): String = "banchus:sessions:$sessionId:stream_offsets"

    companion object {
        private const val BASE_KEY = "banchus:streams"
    }
}
