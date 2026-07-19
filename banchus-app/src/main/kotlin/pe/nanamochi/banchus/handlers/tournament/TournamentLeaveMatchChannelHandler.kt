package pe.nanamochi.banchus.handlers.tournament

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.ChannelName
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.TournamentLeaveMatchChannelPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.stream.StreamName
import pe.nanamochi.banchus.service.ChannelService
import pe.nanamochi.banchus.service.MultiplayerService
import pe.nanamochi.banchus.service.StreamService

@Component
@HandleClientPacket(
    type = PacketType.OSU_TOURNAMENT_LEAVE_MATCH_CHANNEL,
    checkForRestriction = true,
)
class TournamentLeaveMatchChannelHandler(
    private val multiplayerService: MultiplayerService,
    private val streamService: StreamService,
    private val channelService: ChannelService,
) :
    AbstractPacketHandler<TournamentLeaveMatchChannelPacket>(
        PacketType.OSU_TOURNAMENT_LEAVE_MATCH_CHANNEL
    ) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: TournamentLeaveMatchChannelPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.fetchOne(packet.matchId.toLong())?.let { mpMatch ->
            log.info("Tournament client leaving match channel")

            streamService.leave(session.sessionId, StreamName.Multiplayer(mpMatch.matchId))
            channelService.leave(session.sessionId, ChannelName.Multiplayer(mpMatch.matchId))
        }
    }
}
