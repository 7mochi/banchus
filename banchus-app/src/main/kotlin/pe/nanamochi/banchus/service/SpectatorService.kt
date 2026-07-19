package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import java.io.ByteArrayOutputStream
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.database.entity.ChannelName
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.domain.error.InvalidSpectateTarget
import pe.nanamochi.banchus.domain.error.UserNotFound
import pe.nanamochi.banchus.packets.client.SpectateFramesPacket
import pe.nanamochi.banchus.packets.client.StartSpectatingPacket
import pe.nanamochi.banchus.packets.server.ChannelJoinSuccessPacket
import pe.nanamochi.banchus.packets.server.ChannelRevokedPacket
import pe.nanamochi.banchus.packets.server.FellowSpectatorJoinedPacket
import pe.nanamochi.banchus.packets.server.FellowSpectatorLeftPacket
import pe.nanamochi.banchus.packets.server.SpectatorJoinedPacket
import pe.nanamochi.banchus.packets.server.SpectatorLeftPacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.entity.SessionIdentity
import pe.nanamochi.banchus.redis.repository.SpectatorRepository
import pe.nanamochi.banchus.redis.stream.StreamName

@Service
class SpectatorService(
    private val spectatorRepository: SpectatorRepository,
    @Lazy private val sessionService: SessionService,
    private val streamService: StreamService,
    private val channelService: ChannelService,
    private val packetWriter: PacketWriter,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun fetchSpectating(sessionId: UUID) = spectatorRepository.fetchSpectating(sessionId)

    fun fetchAllMembers(hostSessionId: UUID) = spectatorRepository.fetchAllMembers(hostSessionId)

    fun join(session: Session, hostId: Int): Result<Set<SessionIdentity>, DomainMessage> = binding {
        fetchSpectating(session.sessionId)?.let { leave(session, it).bind() }

        val hostSessions = sessionService.fetchByUserId(hostId)
        val hostSession = hostSessions.maxByOrNull { it.updatedAt } ?: Err(UserNotFound).bind()

        val memberCount = spectatorRepository.addMember(hostSession.sessionId, session.identity())
        if (memberCount == 0L) log.error("Unexpected spectators member count of 0")

        val streamName = StreamName.Spectator(hostSession.sessionId)
        val channelName = ChannelName.Spectator(hostSession.sessionId)
        streamService.join(session.sessionId, streamName)
        channelService.join(session, channelName).bind()

        // Notify the host and other spectators about all that
        val hostStreamName = StreamName.User(hostSession.sessionId)
        streamService.broadcastMessage(hostStreamName, SpectatorJoinedPacket(session.userId))

        if (memberCount <= 1L) {
            // We are the first spectator
            // Join the host to their spectator updates stream
            streamService.join(hostSession.sessionId, streamName)
            channelService.join(hostSession, channelName).bind()
            setOf(session.identity())
        } else {
            streamService.broadcastMessage(
                streamName,
                FellowSpectatorJoinedPacket(session.userId),
                listOf(session.sessionId, hostSession.sessionId),
            )
            fetchAllMembers(hostSession.sessionId)
        }
    }

    fun leave(session: Session, hostSessionId: UUID?): Result<Long, DomainMessage> = binding {
        val hostId = hostSessionId ?: fetchSpectating(session.sessionId) ?: return@binding 0L

        val memberCount = spectatorRepository.removeMember(hostId, session.identity())

        val channelName = ChannelName.Spectator(hostId)
        val streamName = StreamName.Spectator(hostId)

        channelService.leave(session.sessionId, channelName).bind()
        streamService.leave(session.sessionId, streamName)

        val hostStreamName = StreamName.User(hostId)
        streamService.broadcastMessage(hostStreamName, SpectatorLeftPacket(session.userId))

        if (memberCount == 0L) {
            streamService.leave(hostId, streamName)
            channelService.leave(hostId, channelName).bind()
            streamService.clearStream(streamName)

            streamService.broadcastMessage(hostStreamName, ChannelRevokedPacket("#spectator"))
        } else {
            streamService.broadcastMessage(
                streamName,
                FellowSpectatorLeftPacket(session.userId),
                listOf(hostId),
            )
        }

        memberCount
    }

    fun close(sessionId: UUID): Result<Unit, DomainMessage> = binding {
        val spectators = fetchAllMembers(sessionId)
        if (spectators.isEmpty()) {
            Ok(Unit)
        }

        val channelName = ChannelName.Spectator(sessionId)
        val streamName = StreamName.Spectator(sessionId)
        spectators.forEach { spectator ->
            spectatorRepository.removeSpectating(spectator.sessionId)
            channelService.leave(spectator.sessionId, channelName).bind()
            streamService.leave(spectator.sessionId, streamName)
        }

        spectatorRepository.removeMembers(sessionId)
        streamService.clearStream(streamName)
    }

    fun handleSpectateFrames(packet: SpectateFramesPacket, session: Session) {
        val streamName = StreamName.Spectator(session.sessionId)
        if (streamService.isJoined(session.sessionId, streamName)) {
            streamService.broadcastMessage(
                streamName,
                pe.nanamochi.banchus.packets.server.SpectateFramesPacket(packet.replayFrameBundle),
                excludedSessionIds = listOf(session.sessionId),
            )
        }
    }

    fun handleStartSpectating(
        packet: StartSpectatingPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ): Result<Unit, DomainMessage> = binding {
        if (packet.userId == 1 || packet.userId == session.userId) {
            Err(InvalidSpectateTarget).bind()
        }

        val specChannelNotifyPacket = ChannelJoinSuccessPacket("#spectator")
        val spectators = join(session, packet.userId).bind()

        if (spectators.size == 1) {
            // we are the only spectator
            responseStream.write(packetWriter.serialize(specChannelNotifyPacket))
        } else {
            val packetsToSerialize = mutableListOf<ServerPacket>()

            spectators
                .filter { it.sessionId != session.sessionId }
                .forEach { other ->
                    packetsToSerialize.add(FellowSpectatorJoinedPacket(other.userId))
                }
            packetsToSerialize.add(specChannelNotifyPacket)

            responseStream.write(packetWriter.serializeAll(packetsToSerialize))
        }
    }
}
