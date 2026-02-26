package pe.nanamochi.banchus.handlers

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.SpectateFramesPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.SpectatorService

@Component
@HandleClientPacket(type = PacketType.OSU_SPECTATE_FRAMES, checkForRestriction = true)
class SpectateFramesHandler(private val spectatorService: SpectatorService) :
    AbstractPacketHandler<SpectateFramesPacket>(PacketType.OSU_SPECTATE_FRAMES) {

    override fun handle(
        packet: SpectateFramesPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {}
}
