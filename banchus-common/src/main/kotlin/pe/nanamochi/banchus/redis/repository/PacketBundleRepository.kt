package pe.nanamochi.banchus.redis.repository

import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.redis.entity.PacketBundle

@Repository
class PacketBundleRepository(private val redisTemplate: RedisTemplate<String, PacketBundle>) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val WARNING_QUEUE_SIZE_THRESHOLD = 100
    }

    fun enqueue(sessionId: UUID, packetBundle: PacketBundle) {
        redisTemplate
            .opsForList()
            .rightPush(makeKey(sessionId), packetBundle)
            ?.takeIf { it > WARNING_QUEUE_SIZE_THRESHOLD }
            ?.also { size ->
                log.warn(
                    "Packet bundle size exceeded warning threshold ($size) for Session: $sessionId"
                )
            }
    }

    fun dequeueOne(sessionId: UUID): PacketBundle? =
        redisTemplate.opsForList().leftPop(makeKey(sessionId))

    fun dequeueAll(sessionId: UUID): List<PacketBundle> =
        makeKey(sessionId).let { key ->
            redisTemplate
                .opsForList()
                .range(key, 0, -1)
                ?.takeIf { it.isNotEmpty() }
                ?.also { redisTemplate.delete(key) } ?: emptyList()
        }

    private fun makeKey(sessionId: UUID): String = "server:packet-bundles:$sessionId"
}
