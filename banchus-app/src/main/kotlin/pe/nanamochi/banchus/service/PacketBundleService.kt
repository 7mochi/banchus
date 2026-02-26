package pe.nanamochi.banchus.service

import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.redis.entity.PacketBundle
import pe.nanamochi.banchus.redis.repository.PacketBundleRepository

@Service
class PacketBundleService(private val packetBundleRepository: PacketBundleRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun enqueue(sessionId: UUID, packetBundle: PacketBundle) =
        packetBundleRepository.enqueue(sessionId, packetBundle)

    fun dequeueAll(sessionId: UUID): List<PacketBundle> =
        packetBundleRepository.dequeueAll(sessionId)
}
