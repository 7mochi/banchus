package pe.nanamochi.banchus.spectator.broadcast

import java.util.UUID
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.SessionIdentity
import pe.nanamochi.banchus.components.ReplayFrameBundle
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.service.StreamService
import pe.nanamochi.banchus.packets.server.ChannelJoinSuccessPacket
import pe.nanamochi.banchus.packets.server.ChannelRevokedPacket
import pe.nanamochi.banchus.packets.server.FellowSpectatorJoinedPacket
import pe.nanamochi.banchus.packets.server.FellowSpectatorLeftPacket
import pe.nanamochi.banchus.packets.server.SpectatorCantSpectatePacket
import pe.nanamochi.banchus.packets.server.SpectatorJoinedPacket
import pe.nanamochi.banchus.packets.server.SpectatorLeftPacket
import pe.nanamochi.banchus.protocol.PacketWriter

@Component
class SpectatorBroadcaster(
    private val packetWriter: PacketWriter,
    private val streamService: StreamService,
) {
    fun startSpectatingResponse(
        spectators: Set<SessionIdentity>,
        session: pe.nanamochi.banchus.auth.entity.Session,
    ): ByteArray {
        return if (spectators.size == 1) {
            packetWriter.serialize(ChannelJoinSuccessPacket("#spectator"))
        } else {
            val packets = buildList {
                spectators
                    .filter { it.sessionId != session.sessionId }
                    .forEach { add(FellowSpectatorJoinedPacket(it.userId)) }
                add(ChannelJoinSuccessPacket("#spectator"))
            }
            packetWriter.serializeAll(packets)
        }
    }

    fun spectatorJoined(stream: StreamName, userId: Int) {
        streamService.broadcastData(stream, packetWriter.serialize(SpectatorJoinedPacket(userId)))
    }

    fun fellowSpectatorJoined(stream: StreamName, userId: Int, excludedSessions: List<UUID>) {
        streamService.broadcastData(
            stream,
            packetWriter.serialize(FellowSpectatorJoinedPacket(userId)),
            excludedSessions,
        )
    }

    fun spectatorLeft(stream: StreamName, userId: Int) {
        streamService.broadcastData(stream, packetWriter.serialize(SpectatorLeftPacket(userId)))
    }

    fun channelRevoked(stream: StreamName) {
        streamService.broadcastData(
            stream,
            packetWriter.serialize(ChannelRevokedPacket("#spectator")),
        )
    }

    fun cantSpectate(stream: StreamName, userId: Int) {
        streamService.broadcastData(
            stream,
            packetWriter.serialize(SpectatorCantSpectatePacket(userId)),
        )
    }

    fun fellowSpectatorLeft(stream: StreamName, userId: Int, excludedSessions: List<UUID>) {
        streamService.broadcastData(
            stream,
            packetWriter.serialize(FellowSpectatorLeftPacket(userId)),
            excludedSessions,
        )
    }

    fun spectateFrames(stream: StreamName, frames: ReplayFrameBundle, excludedSessionId: UUID) {
        streamService.broadcastData(
            stream,
            packetWriter.serialize(
                pe.nanamochi.banchus.packets.server.SpectateFramesPacket(frames)
            ),
            excludedSessionIds = listOf(excludedSessionId),
        )
    }
}
