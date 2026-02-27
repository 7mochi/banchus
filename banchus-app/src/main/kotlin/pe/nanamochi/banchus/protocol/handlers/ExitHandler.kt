package pe.nanamochi.banchus.protocol.handlers

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.ExitPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.SessionService

@Component
@HandleClientPacket(type = PacketType.OSU_EXIT, checkForRestriction = true)
class ExitHandler(private val sessionService: SessionService) :
    AbstractPacketHandler<ExitPacket>(PacketType.OSU_EXIT) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: ExitPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        sessionService.logout(session)
        log.info("User {} has been logged out and cleaned up.", session.user?.username)
    }
}
