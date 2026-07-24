package pe.nanamochi.banchus.multiplayer.handlers.tournament

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.infrastructure.util.asBancho
import pe.nanamochi.banchus.multiplayer.service.MultiplayerService
import pe.nanamochi.banchus.packets.client.TournamentMatchInfoPacket
import pe.nanamochi.banchus.packets.server.MatchUpdatePacket
import pe.nanamochi.banchus.protocol.PacketWriter

@Component
@HandleClientPacket(type = PacketType.OSU_TOURNAMENT_MATCH_INFO, checkForRestriction = true)
class TournamentMatchInfoHandler(
    private val multiplayerService: MultiplayerService,
    private val packetWriter: PacketWriter,
) : AbstractPacketHandler<TournamentMatchInfoPacket>(PacketType.OSU_TOURNAMENT_MATCH_INFO) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: TournamentMatchInfoPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.fetchOne(packet.matchId.toLong())?.let { mpMatch ->
            log.info("Tournament client requesting match info")

            val slots = multiplayerService.fetchAllSlots(mpMatch.matchId)
            responseStream.write(packetWriter.serialize(MatchUpdatePacket(mpMatch.asBancho(slots))))
        }
    }
}
