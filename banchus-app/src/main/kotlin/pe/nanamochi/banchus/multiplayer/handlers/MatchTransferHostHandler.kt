package pe.nanamochi.banchus.multiplayer.handlers

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.multiplayer.broadcast.MultiplayerBroadcaster
import pe.nanamochi.banchus.multiplayer.service.MultiplayerService
import pe.nanamochi.banchus.packets.client.MatchTransferHostPacket

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_TRANSFER_HOST)
class MatchTransferHostHandler(
    private val multiplayerService: MultiplayerService,
    private val broadcaster: MultiplayerBroadcaster,
) : AbstractPacketHandler<MatchTransferHostPacket>(PacketType.OSU_MATCH_TRANSFER_HOST) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchTransferHostPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        if (packet.slotId !in 0..15) return
        val matchId = multiplayerService.fetchSessionMatchId(session.sessionId) ?: return

        multiplayerService
            .transferHostToSlot(matchId, packet.slotId, session.userId)
            .onSuccess { (match, slots) -> broadcaster.matchUpdate(match, slots) }
            .onFailure { error -> log.warn("Failed to transfer host: $error") }
    }
}
