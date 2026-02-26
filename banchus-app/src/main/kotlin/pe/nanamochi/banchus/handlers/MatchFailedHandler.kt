package pe.nanamochi.banchus.handlers

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchFailedPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_FAILED)
class MatchFailedHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchFailedPacket>(PacketType.OSU_MATCH_FAILED) {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchFailedPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {}
}
