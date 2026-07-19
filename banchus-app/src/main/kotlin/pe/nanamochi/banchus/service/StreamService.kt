package pe.nanamochi.banchus.service

import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.repository.StreamRepository
import pe.nanamochi.banchus.redis.stream.MessageInfo
import pe.nanamochi.banchus.redis.stream.StreamName

@Service
class StreamService(
    private val streamRepository: StreamRepository,
    private val packetWriter: PacketWriter,
) {
    fun broadcastData(
        stream: StreamName,
        data: ByteArray,
        excludedSessionIds: List<UUID>? = null,
        readPrivileges: Int? = null,
    ) {
        val info =
            MessageInfo(excludedSessionIds = excludedSessionIds, readPrivileges = readPrivileges)
        streamRepository.xadd(stream.resolve(), data, info)
    }

    fun broadcastMessage(
        stream: StreamName,
        packet: ServerPacket,
        excludedSessionIds: List<UUID>? = null,
        readPrivileges: Int? = null,
    ) {
        broadcastData(stream, packetWriter.serialize(packet), excludedSessionIds, readPrivileges)
    }

    fun readPendingData(session: Session): ByteArray {
        val messages = streamRepository.readPendingMessages(session.sessionId)

        val result = ByteArrayOutputStream()
        for (msg in messages) {
            val isExcluded = msg.info.excludedSessionIds?.contains(session.sessionId) == true
            val readPrivs = msg.info.readPrivileges
            val canRead =
                readPrivs == null || readPrivs == 0 || (session.privileges and readPrivs) != 0

            if (canRead && !isExcluded) {
                result.write(msg.data)
            }
        }

        return result.toByteArray()
    }

    fun join(sessionId: UUID, stream: StreamName) {
        val streamKey = stream.resolve()
        streamRepository.join(sessionId, streamKey)
    }

    fun leave(sessionId: UUID, stream: StreamName) {
        val streamKey = stream.resolve()
        streamRepository.leave(sessionId, streamKey)
    }

    fun leaveAll(sessionId: UUID) {
        streamRepository.leaveAll(sessionId)
    }

    fun isJoined(sessionId: UUID, stream: StreamName): Boolean {
        val streamKey = stream.resolve()
        return streamRepository.isJoined(sessionId, streamKey)
    }

    fun fetchAll(): List<String> {
        return streamRepository.fetchAll()
    }

    fun getLatestMessageTimestamp(stream: StreamName): Instant? {
        val streamKey = stream.resolve()
        val messageId = streamRepository.getLatestMessageId(streamKey) ?: return null

        val timestamp = messageId.split("-").firstOrNull()?.toLongOrNull() ?: return null
        return runCatching { Instant.ofEpochMilli(timestamp) }.getOrNull()
    }

    fun trimStream(stream: StreamName, ttlSeconds: Int): Long {
        val streamKey = stream.resolve()
        val now = Instant.now().toEpochMilli()
        val minId = now - (ttlSeconds * 1000L)
        return streamRepository.trimMessages(streamKey, minId)
    }

    fun clearStream(stream: StreamName) {
        val streamKey = stream.resolve()
        streamRepository.clearStream(streamKey)
    }
}
