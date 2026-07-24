package pe.nanamochi.banchus.multiplayer.handlers

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.error.IncorrectPassword
import pe.nanamochi.banchus.core.error.MultiplayerMatchFull
import pe.nanamochi.banchus.core.service.StreamService
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.infrastructure.util.asBancho
import pe.nanamochi.banchus.multiplayer.service.MultiplayerService
import pe.nanamochi.banchus.packets.client.JoinMatchPacket
import pe.nanamochi.banchus.packets.server.AnnouncePacket
import pe.nanamochi.banchus.packets.server.ChannelJoinSuccessPacket
import pe.nanamochi.banchus.packets.server.MatchJoinFailPacket
import pe.nanamochi.banchus.packets.server.MatchJoinSuccessPacket
import pe.nanamochi.banchus.protocol.PacketWriter

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

                val errorMessage =
                    when (error) {
                        IncorrectPassword -> "You have entered an invalid password for this match."
                        MultiplayerMatchFull -> "The match has no free space left."
                        else -> "An unexpected error occurred."
                    }
                val streamName = StreamName.User(session.sessionId)

                streamService.broadcastData(
                    streamName,
                    packetWriter.serialize(MatchJoinFailPacket()),
                )
                streamService.broadcastData(
                    streamName,
                    packetWriter.serialize(AnnouncePacket(errorMessage)),
                )
            }
    }
}
