package pe.nanamochi.banchus.handlers.multiplayer

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.toResultOr
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.domain.error.NotInMatch
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchInvitePacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_INVITE)
class MatchInviteHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchInvitePacket>(PacketType.OSU_MATCH_INVITE) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchInvitePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        binding {
                val matchId =
                    multiplayerService
                        .fetchSessionMatchId(session.sessionId)
                        .toResultOr { NotInMatch }
                        .bind()

                multiplayerService.invitePlayerToMatch(matchId, session, packet.userId).bind()

                Ok(Unit)
            }
            .onFailure { error -> log.warn("Failed to invite player to match") }
    }
}
