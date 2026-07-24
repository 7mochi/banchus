package pe.nanamochi.banchus.score.handlers

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.service.PresenceService
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.infrastructure.util.toBanchoUser
import pe.nanamochi.banchus.packets.client.UserStatsRequestPacket
import pe.nanamochi.banchus.packets.server.UserQuitPacket
import pe.nanamochi.banchus.packets.server.UserStatsPacket
import pe.nanamochi.banchus.protocol.PacketWriter

@Component
@HandleClientPacket(type = PacketType.OSU_USER_STATS_REQUEST, checkForRestriction = true)
class UserStatsRequestHandler(
    private val presenceService: PresenceService,
    private val packetWriter: PacketWriter,
) : AbstractPacketHandler<UserStatsRequestPacket>(PacketType.OSU_USER_STATS_REQUEST) {
    override fun handle(
        packet: UserStatsRequestPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        presenceService.getUserStats(packet.userIds).forEach { (userId, presence) ->
            presence?.let { p ->
                if (p.globalRank > 0u) {
                    responseStream.write(
                        packetWriter.serialize(UserStatsPacket(user = p.toBanchoUser()))
                    )
                }
            } ?: run { responseStream.write(packetWriter.serialize(UserQuitPacket(userId))) }
        }
    }
}
