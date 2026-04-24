package pe.nanamochi.banchus.handlers.multiplayer

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toResultOr
import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.domain.error.MatchNotFound
import pe.nanamochi.banchus.domain.error.SlotNotFound
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchTransferHostPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_TRANSFER_HOST)
class MatchTransferHostHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchTransferHostPacket>(PacketType.OSU_MATCH_TRANSFER_HOST) {
    override fun handle(
        packet: MatchTransferHostPacket,
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

            multiplayerService.transferHostToSlot(matchId, packet.slotId, session.userId).bind()
        }
    }
}
