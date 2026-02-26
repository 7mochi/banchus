package pe.nanamochi.banchus.service

import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import pe.nanamochi.banchus.packets.server.*
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.repository.SpectatorRepository

@Service
class SpectatorService(
    private val transactionTemplate: TransactionTemplate,
    private val spectatorRepository: SpectatorRepository,
    private val channelService: ChannelService,
    private val sessionService: SessionService,
    private val packetBundleService: PacketBundleService,
    private val packetWriter: PacketWriter,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getSpectatorIds(hostId: UUID): Set<UUID> = spectatorRepository.getMembers(hostId)
}
