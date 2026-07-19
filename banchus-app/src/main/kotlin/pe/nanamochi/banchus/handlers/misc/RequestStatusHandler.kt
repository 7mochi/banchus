package pe.nanamochi.banchus.handlers.misc

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.RequestStatusPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.PresenceService

@Component
@HandleClientPacket(type = PacketType.OSU_REQUEST_STATUS)
class RequestStatusHandler(private val presenceService: PresenceService) :
    AbstractPacketHandler<RequestStatusPacket>(PacketType.OSU_REQUEST_STATUS) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: RequestStatusPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        presenceService.handleRequestStatus(session, responseStream).onFailure {
            log.warn(
                "Error requesting status for user ${session.username} (${session.userId}): $it"
            )
        }
    }
}
