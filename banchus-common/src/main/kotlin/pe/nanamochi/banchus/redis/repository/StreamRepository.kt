package pe.nanamochi.banchus.redis.repository

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.Limit
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.connection.stream.StreamReadOptions
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.redis.stream.MessageInfo

@Repository
class StreamRepository(
    private val byteArrayRedisTemplate: RedisTemplate<String, ByteArray>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun xadd(streamKey: String, data: ByteArray, info: MessageInfo): RecordId? {
        val infoJson = objectMapper.writeValueAsString(info)
        val record =
            StreamRecords.newRecord()
                .ofMap(mapOf("data" to data, "info" to infoJson.toByteArray()))
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
                try {
                    streamOps.read(
                        StreamReadOptions.empty().count(100),
                        org.springframework.data.redis.connection.stream.StreamOffset.create(
                            streamKey,
                            org.springframework.data.redis.connection.stream.ReadOffset.from(lastId),
                        ),
                    )
                } catch (_: Exception) {
                    null
                } ?: continue

            for (message in messages) {
                val data = message.value["data"] ?: continue
                val infoBytes = message.value["info"] ?: continue
                val info = deserializeMessageInfo(String(infoBytes, Charsets.UTF_8))

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
        return try {
            val streamOps = byteArrayRedisTemplate.opsForStream<String, ByteArray>()
            streamOps.trim(streamKey, minId)
        } catch (e: Exception) {
            log.warn("Failed to trim stream $streamKey: ${e.message}")
            0L
        }
    }

    data class PendingMessage(val data: ByteArray, val info: MessageInfo)

    private fun deserializeMessageInfo(raw: String?): MessageInfo {
        if (raw.isNullOrBlank()) return MessageInfo()
        return try {
            objectMapper.readValue(raw, MessageInfo::class.java)
        } catch (e: Exception) {
            log.warn("Failed to deserialize MessageInfo: ${e.message}")
            MessageInfo()
        }
    }

    private fun makeKey(sessionId: UUID): String = "banchus:sessions:$sessionId:stream_offsets"

    companion object {
        private const val BASE_KEY = "banchus:streams"
    }
}
