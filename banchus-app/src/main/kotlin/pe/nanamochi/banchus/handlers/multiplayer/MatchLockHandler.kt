package pe.nanamochi.banchus.handlers.multiplayer

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toResultOr
import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.domain.enums.SlotStatus
import pe.nanamochi.banchus.domain.error.NotInMatch
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchLockPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_LOCK)
class MatchLockHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchLockPacket>(PacketType.OSU_MATCH_LOCK) {
    override fun handle(
        packet: MatchLockPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        binding {
            val matchId =
                multiplayerService
                    .fetchSessionMatchId(session.sessionId)
                    .toResultOr { NotInMatch }
                    .bind()
            multiplayerService.setSlotStatus(
                matchId,
                packet.slotId,
                SlotStatus.LOCKED,
                session.userId,
            )
        }
    }
}
