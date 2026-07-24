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
import pe.nanamochi.banchus.infrastructure.util.userPanel
import pe.nanamochi.banchus.packets.client.ChangeStatusPacket
import pe.nanamochi.banchus.protocol.PacketWriter

@Component
@HandleClientPacket(type = PacketType.OSU_CHANGE_STATUS, checkForRestriction = true)
class ChangeStatusHandler(
    private val presenceService: PresenceService,
    private val packetWriter: PacketWriter,
    private val streamService: StreamService,
) : AbstractPacketHandler<ChangeStatusPacket>(PacketType.OSU_CHANGE_STATUS) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: ChangeStatusPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        presenceService
            .changeStatus(packet.statusUpdate, session)
            .onSuccess { presence ->
                if (!session.isRestricted) {
                    presence.userPanel().forEach { p ->
                        streamService.broadcastData(StreamName.Main, packetWriter.serialize(p))
                    }
                }
            }
            .onFailure { error ->
                log.warn(
                    "Failed to update and broadcast status for user {}: {}",
                    session.username,
                    error,
                )
            }
    }
}
