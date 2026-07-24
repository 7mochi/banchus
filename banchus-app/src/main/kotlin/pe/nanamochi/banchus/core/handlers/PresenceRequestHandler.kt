package pe.nanamochi.banchus.core.handlers

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.service.PresenceService
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.infrastructure.util.userPanel
import pe.nanamochi.banchus.packets.client.PresenceRequestPacket
import pe.nanamochi.banchus.packets.server.UserQuitPacket
import pe.nanamochi.banchus.protocol.PacketWriter

@Component
@HandleClientPacket(type = PacketType.OSU_PRESENCE_REQUEST, checkForRestriction = true)
class PresenceRequestHandler(
    private val presenceService: PresenceService,
    private val packetWriter: PacketWriter,
) : AbstractPacketHandler<PresenceRequestPacket>(PacketType.OSU_PRESENCE_REQUEST) {
    override fun handle(
        packet: PresenceRequestPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        presenceService.getPresences(packet.userIds).forEach { (userId, presence) ->
            presence?.let { p -> responseStream.write(packetWriter.serializeAll(p.userPanel())) }
                ?: run { responseStream.write(packetWriter.serialize(UserQuitPacket(userId))) }
        }
    }
}
