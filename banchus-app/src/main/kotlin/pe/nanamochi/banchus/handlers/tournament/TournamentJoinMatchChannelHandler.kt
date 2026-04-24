package pe.nanamochi.banchus.handlers.tournament

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.ChannelName
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.TournamentJoinMatchChannelPacket
import pe.nanamochi.banchus.packets.server.MatchUpdatePacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.stream.StreamName
import pe.nanamochi.banchus.service.ChannelService
import pe.nanamochi.banchus.service.MultiplayerService
import pe.nanamochi.banchus.service.StreamService
import pe.nanamochi.banchus.util.asBancho

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
