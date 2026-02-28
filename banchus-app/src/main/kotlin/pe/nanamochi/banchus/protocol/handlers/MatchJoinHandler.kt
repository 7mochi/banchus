package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.domain.errors.IncorrectPassword
import pe.nanamochi.banchus.domain.errors.MatchNotFound
import pe.nanamochi.banchus.domain.errors.SlotNotAvailable
import pe.nanamochi.banchus.domain.errors.UserSilenced
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchJoinPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_JOIN)
class MatchJoinHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchJoinPacket>(PacketType.OSU_MATCH_JOIN) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchJoinPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.joinMatchFromJoinPacket(session, packet).onFailure { error ->
            log.warn("Join match failed for user {}: {}", session.user?.username, error)
            when (error) {
                UserSilenced -> {
                    multiplayerService.sendMatchJoinFail(
                        session,
                        "Multiplayer is not available while silenced.",
                    )
                }
                IncorrectPassword,
                MatchNotFound,
                SlotNotAvailable -> {
                    multiplayerService.sendMatchJoinFail(session, null)
                }
                else -> {
                    multiplayerService.sendMatchJoinFail(session, "An unexpected error occurred.")
                }
            }
        }
    }
}
