package pe.nanamochi.banchus.core.handlers

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.service.PresenceService
import pe.nanamochi.banchus.core.service.StreamService
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.infrastructure.util.toBanchoUser
import pe.nanamochi.banchus.packets.client.RequestStatusPacket
import pe.nanamochi.banchus.packets.server.UserStatsPacket
import pe.nanamochi.banchus.protocol.PacketWriter

@Component
@HandleClientPacket(type = PacketType.OSU_REQUEST_STATUS)
class RequestStatusHandler(
    private val presenceService: PresenceService,
    private val packetWriter: PacketWriter,
    private val streamService: StreamService,
) : AbstractPacketHandler<RequestStatusPacket>(PacketType.OSU_REQUEST_STATUS) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: RequestStatusPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        presenceService
            .getRequestStatus(session)
            .onSuccess { presence ->
                val statsPacket = packetWriter.serialize(UserStatsPacket(presence.toBanchoUser()))
                if (!session.isRestricted) {
                    streamService.broadcastData(StreamName.Main, statsPacket)
                } else {
                    responseStream.write(statsPacket)
                }
            }
            .onFailure {
                log.warn(
                    "Error requesting status for user ${session.username} (${session.userId}): $it"
                )
            }
    }
}
