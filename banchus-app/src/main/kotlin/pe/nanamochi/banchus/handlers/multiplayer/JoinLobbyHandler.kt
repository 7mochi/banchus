package pe.nanamochi.banchus.handlers.multiplayer

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.JoinLobbyPacket
import pe.nanamochi.banchus.packets.server.MatchUpdatePacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.stream.StreamName
import pe.nanamochi.banchus.service.MultiplayerService
import pe.nanamochi.banchus.service.StreamService
import pe.nanamochi.banchus.util.asBancho

@Component
@HandleClientPacket(type = PacketType.OSU_JOIN_LOBBY)
class JoinLobbyHandler(
    private val multiplayerService: MultiplayerService,
    private val streamService: StreamService,
    private val packetWriter: PacketWriter,
) : AbstractPacketHandler<JoinLobbyPacket>(PacketType.OSU_JOIN_LOBBY) {
    override fun handle(
        packet: JoinLobbyPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        streamService.join(session.sessionId, StreamName.Lobby)
        val matches = multiplayerService.fetchAllWithSlots()
        matches.forEach { (mpMatch, slots) ->
            responseStream.write(packetWriter.serialize(MatchUpdatePacket(mpMatch.asBancho(slots))))
        }
    }
}
