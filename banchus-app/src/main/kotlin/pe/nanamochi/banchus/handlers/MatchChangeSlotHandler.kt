package pe.nanamochi.banchus.handlers

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchChangeSlotPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_CHANGE_SLOT)
class MatchChangeSlotHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchChangeSlotPacket>(PacketType.OSU_MATCH_CHANGE_SLOT) {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchChangeSlotPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {}
}
