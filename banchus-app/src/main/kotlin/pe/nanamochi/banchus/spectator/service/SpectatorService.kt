package pe.nanamochi.banchus.spectator.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.auth.entity.SessionIdentity
import pe.nanamochi.banchus.auth.service.SessionService
import pe.nanamochi.banchus.chat.entity.ChannelName
import pe.nanamochi.banchus.chat.service.ChannelService
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.UserNotFound
import pe.nanamochi.banchus.core.service.StreamService
import pe.nanamochi.banchus.spectator.broadcast.SpectatorBroadcaster
import pe.nanamochi.banchus.spectator.repository.SpectatorRepository

@Service
class SpectatorService(
    private val spectatorRepository: SpectatorRepository,
    @Lazy private val sessionService: SessionService,
    private val streamService: StreamService,
    private val channelService: ChannelService,
    private val broadcaster: SpectatorBroadcaster,
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

        val hostStreamName = StreamName.User(hostSession.sessionId)
        broadcaster.spectatorJoined(hostStreamName, session.userId)

        if (memberCount <= 1L) {
            streamService.join(hostSession.sessionId, streamName)
            channelService.join(hostSession, channelName).bind()
            setOf(session.identity())
        } else {
            broadcaster.fellowSpectatorJoined(
                streamName,
                session.userId,
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
        broadcaster.spectatorLeft(hostStreamName, session.userId)

        if (memberCount == 0L) {
            streamService.leave(hostId, streamName)
            channelService.leave(hostId, channelName).bind()
            streamService.clearStream(streamName)

            broadcaster.channelRevoked(hostStreamName)
        } else {
            broadcaster.fellowSpectatorLeft(streamName, session.userId, listOf(hostId))
        }

        memberCount
    }

    fun close(sessionId: UUID): Result<Unit, DomainMessage> = binding {
        val spectators = fetchAllMembers(sessionId)
        if (spectators.isNotEmpty()) {
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
    }

    fun isSpectating(sessionId: UUID, streamName: StreamName): Boolean =
        streamService.isJoined(sessionId, streamName)
}
