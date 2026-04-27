package pe.nanamochi.banchus.handlers.multiplayer

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.domain.error.InvalidPassword
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.JoinMatchPacket
import pe.nanamochi.banchus.packets.server.AnnouncePacket
import pe.nanamochi.banchus.packets.server.ChannelJoinSuccessPacket
import pe.nanamochi.banchus.packets.server.MatchJoinFailPacket
import pe.nanamochi.banchus.packets.server.MatchJoinSuccessPacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.stream.StreamName
import pe.nanamochi.banchus.service.MultiplayerService
import pe.nanamochi.banchus.service.StreamService
import pe.nanamochi.banchus.util.asBancho

@Component
@HandleClientPacket(type = PacketType.OSU_JOIN_MATCH)
class JoinMatchHandler(
    private val multiplayerService: MultiplayerService,
    private val packetWriter: PacketWriter,
    private val streamService: StreamService,
) : AbstractPacketHandler<JoinMatchPacket>(PacketType.OSU_JOIN_MATCH) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: JoinMatchPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService
            .join(session, packet.matchId.toLong(), packet.matchPassword)
            .onSuccess { (mpMatch, slots) ->
                responseStream.write(
                    packetWriter.serializeAll(
                        listOf(
                            MatchJoinSuccessPacket(mpMatch.asBancho(slots)),
                            ChannelJoinSuccessPacket("#multiplayer"),
                        )
                    )
                )
            }
            .onFailure { error ->
                log.warn("User ${session.username} failed to join match ${packet.matchId}: $error")
                when (error) {
                    InvalidPassword ->
                        streamService.broadcastMessage(
                            StreamName.User(session.sessionId),
                            MatchJoinFailPacket(),
                        )
                    else -> {
                        streamService.broadcastMessage(
                            StreamName.User(session.sessionId),
                            AnnouncePacket("An unexpected error occurred."),
                        )
                    }
                }
            }
    }
}
