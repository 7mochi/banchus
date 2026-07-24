package pe.nanamochi.banchus.spectator.handlers

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.CantSpectatePacket
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.spectator.broadcast.SpectatorBroadcaster
import pe.nanamochi.banchus.spectator.service.SpectatorService

@Component
@HandleClientPacket(type = PacketType.OSU_CANT_SPECTATE, checkForRestriction = true)
class CantSpectateHandler(
    private val spectatorService: SpectatorService,
    private val broadcaster: SpectatorBroadcaster,
) : AbstractPacketHandler<CantSpectatePacket>(PacketType.OSU_CANT_SPECTATE) {
    override fun handle(
        packet: CantSpectatePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val hostSessionId = spectatorService.fetchSpectating(session.sessionId) ?: return
        broadcaster.cantSpectate(StreamName.User(hostSessionId), session.userId)
    }
}
