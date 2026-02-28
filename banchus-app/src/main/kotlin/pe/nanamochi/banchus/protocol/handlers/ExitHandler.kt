package pe.nanamochi.banchus.protocol.handlers

import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.Instant
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
        // The osu! client will often attempt to logout as soon as they login,
        // this is a quirk of the client, and we don't really want to log them out;
        // so we ignore this case if it's been < 1 second since the client's login
        val sessionAge = Duration.between(session.createdAt, Instant.now())
        if (sessionAge < Duration.ofSeconds(1)) {
            log.debug(
                "Ignoring logout attempt < 1 second after login for user {}",
                session.user?.username,
            )
            return
        }

        sessionService.logout(session)
        log.info("User {} has been logged out and cleaned up.", session.user?.username)
    }
}
