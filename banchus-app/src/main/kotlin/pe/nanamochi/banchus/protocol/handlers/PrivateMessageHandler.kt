package pe.nanamochi.banchus.protocol.handlers

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.PrivateMessagePacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler

@Component
@HandleClientPacket(type = PacketType.OSU_PRIVATE_MESSAGE, checkForRestriction = true)
class PrivateMessageHandler :
    AbstractPacketHandler<PrivateMessagePacket>(PacketType.OSU_PRIVATE_MESSAGE) {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: PrivateMessagePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {}
}
