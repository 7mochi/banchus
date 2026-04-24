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
class StreamRepository(private val redisTemplate: RedisTemplate<String, String>) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()

    fun xadd(streamKey: String, data: ByteArray, info: MessageInfo): RecordId? {
        val infoJson = objectMapper.writeValueAsString(info)
        val record =
            StreamRecords.newRecord()
                .ofMap(mapOf("data" to String(data, Charsets.ISO_8859_1), "info" to infoJson))
                .withStreamKey(streamKey)

        return redisTemplate.opsForStream<String, Any>().add(record)
    }

    fun join(sessionId: UUID, streamKey: String) {
        val latestId = getLatestMessageId(streamKey) ?: "0-0"
        redisTemplate.opsForHash<String, String>().put(makeKey(sessionId), streamKey, latestId)
    }

    fun leave(sessionId: UUID, streamKey: String) {
        redisTemplate.opsForHash<String, String>().delete(makeKey(sessionId), streamKey)
    }

    fun leaveAll(sessionId: UUID) {
        redisTemplate.delete(makeKey(sessionId))
    }

    fun isJoined(sessionId: UUID, streamKey: String): Boolean {
        return redisTemplate.opsForHash<String, String>().hasKey(makeKey(sessionId), streamKey)
    }

    fun readPendingMessages(sessionId: UUID): List<PendingMessage> {
        val offsetsKey = makeKey(sessionId)
        val offsets = redisTemplate.opsForHash<String, String>().entries(offsetsKey)

        if (offsets.isEmpty()) return emptyList()

        val allMessages = mutableListOf<PendingMessage>()
        val updatedOffsets = offsets.toMutableMap()

        val streamOps = redisTemplate.opsForStream<String, Any>()

        for ((streamKey, lastId) in offsets) {
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
                val data = message.value["data"] as? String ?: continue
                val info = deserializeMessageInfo(message.value["info"] as? String)

                allMessages.add(
                    PendingMessage(data = data.toByteArray(Charsets.ISO_8859_1), info = info)
                )
            }

            messages.lastOrNull()?.id?.let { updatedOffsets[streamKey] = it.value }
        }

        if (updatedOffsets.isNotEmpty()) {
            redisTemplate.opsForHash<String, String>().putAll(offsetsKey, updatedOffsets)
        }

        return allMessages
    }

    fun clearStream(streamKey: String) {
        redisTemplate.delete(streamKey)
    }

    fun getLatestMessageId(streamKey: String): String? {
        val messages =
            redisTemplate
                .opsForStream<String, Any>()
                .reverseRange(streamKey, Range.closed("+", "-"), Limit.limit().count(1))
                ?: return null
        return messages.lastOrNull()?.id?.value ?: "0-0"
    }

    fun fetchAll(): List<String> {
        val keys = mutableListOf<String>()
        val scanCursor =
            redisTemplate.scan(
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
            @Suppress("UNCHECKED_CAST") val streamOps = redisTemplate.opsForStream<String, Any>()
            streamOps.trim(streamKey, minId) as? Long ?: 0L
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
