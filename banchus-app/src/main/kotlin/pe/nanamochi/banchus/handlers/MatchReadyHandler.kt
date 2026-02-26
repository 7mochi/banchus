package pe.nanamochi.banchus.handlers

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchReadyPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_READY)
class MatchReadyHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchReadyPacket>(PacketType.OSU_MATCH_READY) {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchReadyPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {}
}
