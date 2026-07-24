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
import pe.nanamochi.banchus.packets.client.AddFriendPacket

@Component
@HandleClientPacket(type = PacketType.OSU_ADD_FRIEND)
class AddFriendHandler(private val relationshipService: RelationshipService) :
    AbstractPacketHandler<AddFriendPacket>(PacketType.OSU_ADD_FRIEND) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: AddFriendPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        relationshipService.addFriend(session.userId, packet.userId).onFailure {
            log.warn("User {} failed to add friend {}: {}", session.userId, packet.userId, it)
        }
    }
}
