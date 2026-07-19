package pe.nanamochi.banchus.handlers.multiplayer

import com.github.michaelbull.result.onSuccess
import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.LeaveMatchPacket
import pe.nanamochi.banchus.packets.server.ChannelRevokedPacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_LEAVE_MATCH)
class LeaveMatchHandler(
    private val multiplayerService: MultiplayerService,
    private val packetWriter: PacketWriter,
) : AbstractPacketHandler<LeaveMatchPacket>(PacketType.OSU_LEAVE_MATCH) {
    override fun handle(
        packet: LeaveMatchPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.leave(session.identity()).onSuccess {
            responseStream.write(
                packetWriter.serialize(ChannelRevokedPacket(channelName = "#multiplayer"))
            )
        }
    }
}
