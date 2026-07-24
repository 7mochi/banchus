package pe.nanamochi.banchus.core.handlers

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.infrastructure.util.userPanel
import pe.nanamochi.banchus.packets.client.PresenceRequestAllPacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.core.service.PresenceService

@Component
@HandleClientPacket(type = PacketType.OSU_PRESENCE_REQUEST_ALL, checkForRestriction = true)
class PresenceRequestAllHandler(
    private val presenceService: PresenceService,
    private val packetWriter: PacketWriter,
) : AbstractPacketHandler<PresenceRequestAllPacket>(PacketType.OSU_PRESENCE_REQUEST_ALL) {
    override fun handle(
        packet: PresenceRequestAllPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        presenceService.fetchAll().forEach { presence ->
            responseStream.write(packetWriter.serializeAll(presence.userPanel()))
        }
    }
}
