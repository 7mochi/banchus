package pe.nanamochi.banchus.identity.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.identity.service.RelationshipService
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.RemoveFriendPacket

@Component
@HandleClientPacket(type = PacketType.OSU_REMOVE_FRIEND)
class RemoveFriendHandler(private val relationshipService: RelationshipService) :
    AbstractPacketHandler<RemoveFriendPacket>(PacketType.OSU_REMOVE_FRIEND) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: RemoveFriendPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        relationshipService.removeFriend(session.userId, packet.userId).onFailure {
            log.warn("User {} failed to remove friend {}: {}", session.userId, packet.userId, it)
        }
    }
}
