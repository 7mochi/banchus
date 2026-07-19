package pe.nanamochi.banchus.handlers.multiplayer

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.toResultOr
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.domain.error.MatchNotFound
import pe.nanamochi.banchus.domain.error.SlotNotFound
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchChangeSlotPacket
import pe.nanamochi.banchus.redis.entity.Session
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
    ) {
        binding {
                if (packet.slotId !in 0..15) Err(SlotNotFound).bind<Unit>()
                val matchId =
                    multiplayerService
                        .fetchSessionMatchId(session.sessionId)
                        .toResultOr { MatchNotFound }
                        .bind()

                multiplayerService
                    .swapSessionSlots(matchId, packet.slotId, session.sessionId)
                    .bind()

                Ok(Unit)
            }
            .onFailure { error ->
                log.warn("Failed to change slot for session ${session.sessionId}: $error")
            }
    }
}
