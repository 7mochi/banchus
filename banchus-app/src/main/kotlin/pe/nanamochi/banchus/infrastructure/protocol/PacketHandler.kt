package pe.nanamochi.banchus.infrastructure.protocol

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.protocol.AbstractPacketHandler

@Component
class PacketHandler(beans: List<AbstractPacketHandler<*>>) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val handlers: Map<PacketType, AbstractPacketHandler<*>> = beans.associateBy { it.type }

    fun handle(
        packet: BanchoPacket.Client,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val handler = handlers[packet.type] ?: return

        val annotation =
            handler::class.annotations.find { it is HandleClientPacket } as? HandleClientPacket
        if (annotation?.checkForRestriction == true && session.user?.isRestricted == true) {
            return
        }

        log.debug(
            "Handling packet of type {} with handler {}",
            packet.type,
            handler::class.simpleName,
        )
        dispatch(handler, packet, session, responseStream)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : BanchoPacket.Client> dispatch(
        handler: AbstractPacketHandler<T>,
        packet: BanchoPacket.Client,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        handler.handle(packet as T, session, responseStream)
    }
}
