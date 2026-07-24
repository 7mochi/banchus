package pe.nanamochi.banchus.multiplayer.handlers.tournament

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.chat.entity.ChannelName
import pe.nanamochi.banchus.chat.service.ChannelService
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.infrastructure.util.asBancho
import pe.nanamochi.banchus.multiplayer.service.MultiplayerService
import pe.nanamochi.banchus.packets.client.TournamentJoinMatchChannelPacket
import pe.nanamochi.banchus.packets.server.MatchUpdatePacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.service.StreamService

@Component
@HandleClientPacket(type = PacketType.OSU_TOURNAMENT_JOIN_MATCH_CHANNEL, checkForRestriction = true)
class TournamentJoinMatchChannelHandler(
    private val multiplayerService: MultiplayerService,
    private val streamService: StreamService,
    private val channelService: ChannelService,
    private val packetWriter: PacketWriter,
) :
    AbstractPacketHandler<TournamentJoinMatchChannelPacket>(
        PacketType.OSU_TOURNAMENT_JOIN_MATCH_CHANNEL
    ) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: TournamentJoinMatchChannelPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.fetchOne(packet.matchId.toLong())?.let { mpMatch ->
            log.info("Tournament client joining match channel")

            streamService.join(session.sessionId, StreamName.Multiplayer(mpMatch.matchId))

            if (session.isTournamentStaff) {
                channelService.join(session, ChannelName.Multiplayer(mpMatch.matchId))
            }

            val slots = multiplayerService.fetchAllSlots(mpMatch.matchId)
            responseStream.write(packetWriter.serialize(MatchUpdatePacket(mpMatch.asBancho(slots))))
        }
    }
}
