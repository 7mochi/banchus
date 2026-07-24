package pe.nanamochi.banchus.infrastructure.protocol

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType

@Component
class PacketHandler(beans: List<AbstractPacketHandler<*>>) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val handlers: Map<PacketType, AbstractPacketHandler<*>> = beans.associateBy { it.type }

    fun handle(packet: ClientPacket, session: Session, responseStream: ByteArrayOutputStream) {
        val handler = handlers[packet.type] ?: return

        val annotation =
            handler::class.annotations.find { it is HandleClientPacket } as? HandleClientPacket
        if (annotation?.checkForRestriction == true && session.isRestricted == true) {
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
    private fun <T : ClientPacket> dispatch(
        handler: AbstractPacketHandler<T>,
        packet: ClientPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        handler.handle(packet as T, session, responseStream)
    }
}
