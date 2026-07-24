package pe.nanamochi.banchus.spectator.handlers

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.SpectateFramesPacket
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.spectator.broadcast.SpectatorBroadcaster
import pe.nanamochi.banchus.spectator.service.SpectatorService

@Component
@HandleClientPacket(type = PacketType.OSU_SPECTATE_FRAMES, checkForRestriction = true)
class SpectateFramesHandler(
    private val spectatorService: SpectatorService,
    private val broadcaster: SpectatorBroadcaster,
) : AbstractPacketHandler<SpectateFramesPacket>(PacketType.OSU_SPECTATE_FRAMES) {
    override fun handle(
        packet: SpectateFramesPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val streamName = StreamName.Spectator(session.sessionId)
        if (spectatorService.isSpectating(session.sessionId, streamName)) {
            broadcaster.spectateFrames(streamName, packet.replayFrameBundle, session.sessionId)
        }
    }
}
