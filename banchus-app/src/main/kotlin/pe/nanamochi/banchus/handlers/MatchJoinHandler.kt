package pe.nanamochi.banchus.handlers

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.dto.MatchJoinData
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchJoinPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_JOIN)
class MatchJoinHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchJoinPacket>(PacketType.OSU_MATCH_JOIN) {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchJoinPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val data = MatchJoinData(packet.matchId, packet.matchPassword)
    }
}
