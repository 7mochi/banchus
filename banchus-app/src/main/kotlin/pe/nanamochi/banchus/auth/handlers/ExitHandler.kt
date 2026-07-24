package pe.nanamochi.banchus.auth.handlers

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.auth.service.SessionService
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.ExitPacket
import pe.nanamochi.banchus.packets.server.UserQuitPacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.service.StreamService

@Component
@HandleClientPacket(type = PacketType.OSU_EXIT)
class ExitHandler(
    private val sessionService: SessionService,
    private val packetWriter: PacketWriter,
    private val streamService: StreamService,
) : AbstractPacketHandler<ExitPacket>(PacketType.OSU_EXIT) {
    override fun handle(
        packet: ExitPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        if (sessionService.logout(session)) {
            streamService.broadcastData(
                StreamName.Main,
                packetWriter.serialize(UserQuitPacket(session.userId)),
            )
        }
    }
}
