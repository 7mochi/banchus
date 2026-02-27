package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.toResultOr
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Channel
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.domain.enums.ServerPrivileges
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.InvalidSpectateTarget
import pe.nanamochi.banchus.domain.errors.SessionNotFound
import pe.nanamochi.banchus.domain.errors.UserNotFound
import pe.nanamochi.banchus.events.UserLogoutEvent
import pe.nanamochi.banchus.packets.client.StartSpectatingPacket
import pe.nanamochi.banchus.packets.server.FellowSpectatorJoinedPacket
import pe.nanamochi.banchus.packets.server.FellowSpectatorLeftPacket
import pe.nanamochi.banchus.packets.server.SpectatorJoinedPacket
import pe.nanamochi.banchus.packets.server.SpectatorLeftPacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.PacketBundle
import pe.nanamochi.banchus.redis.repository.SpectatorRepository

@Service
class SpectatorService(
    private val spectatorRepository: SpectatorRepository,
    private val channelService: ChannelService,
    private val sessionService: SessionService,
    private val packetBundleService: PacketBundleService,
    private val packetWriter: PacketWriter,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun addSpectator(hostSessionId: UUID, spectatorSessionId: UUID): UUID {
        log.debug("Adding spectator {} to host {}", spectatorSessionId, hostSessionId)
        return spectatorRepository.add(hostSessionId, spectatorSessionId)
    }

    fun removeSpectator(hostSessionId: UUID, spectatorSessionId: UUID) {
        log.debug("Removing spectator {} from host {}", spectatorSessionId, hostSessionId)
        spectatorRepository.remove(hostSessionId, spectatorSessionId)
    }

    fun getSpectatorIds(hostId: UUID): Set<UUID> = spectatorRepository.getMembers(hostId)

    fun startSpectating(
        spectator: Session,
        packet: StartSpectatingPacket,
    ): Result<Unit, DomainMessage> {
        return binding {
            val spectatorUser = spectator.user ?: Err(UserNotFound).bind()
            val spectatorSessionId = spectator.id ?: Err(SessionNotFound).bind()

            val hostSession = sessionService.findPrimaryByUserId(packet.userId).bind()

            val hostUser = hostSession.user ?: Err(UserNotFound).bind()
            val hostSessionId = hostSession.id ?: Err(SessionNotFound).bind()

            if (hostUser.id == spectatorUser.id || hostUser.id <= 1) {
                Err(InvalidSpectateTarget).bind()
            }

            addSpectator(hostSessionId, spectatorSessionId)

            spectator.spectatorHostSessionId = hostSessionId
            sessionService.update(spectator)

            val channelName = "#spec_$hostSessionId"

            channelService.findByName(channelName).onFailure {
                val channel =
                    Channel().apply {
                        name = channelName
                        topic = "Channel for spectator host ID $hostSessionId"
                        readPrivileges = ServerPrivileges.UNRESTRICTED.value
                        writePrivileges = ServerPrivileges.UNRESTRICTED.value
                        autoJoin = false
                        temporary = true
                    }
                channelService.create(channel)
                channelService.joinChannel(hostSession, channelName, true)
            }

            channelService.joinChannel(spectator, channelName, true)
            packetBundleService.enqueue(
                hostSessionId,
                PacketBundle(packetWriter.serialize(SpectatorJoinedPacket(spectatorUser.id))),
            )

            getSpectatorIds(hostSessionId)
                .filter { it != spectatorSessionId }
                .forEach { otherSpecId ->
                    packetBundleService.enqueue(
                        otherSpecId,
                        PacketBundle(
                            packetWriter.serialize(FellowSpectatorJoinedPacket(spectatorUser.id))
                        ),
                    )
                }

            log.info("User {} started spectating {}", spectatorUser.username, hostUser.username)
        }
    }

    fun stopSpectating(spectator: Session): Result<Unit, DomainMessage> {
        return binding {
            val spectatorUser = spectator.user ?: Err(UserNotFound).bind()
            val spectatorSessionId = spectator.id ?: Err(SessionNotFound).bind()

            val hostSessionId =
                spectator.spectatorHostSessionId.toResultOr { SessionNotFound }.bind()
            val hostSession = sessionService.findById(hostSessionId).bind()

            removeSpectator(hostSessionId, spectatorSessionId)
            spectator.spectatorHostSessionId = null
            sessionService.update(spectator)

            val channelName = "#spec_$hostSessionId"
            channelService.leaveChannel(spectator, channelName)

            packetBundleService.enqueue(
                hostSessionId,
                PacketBundle(packetWriter.serialize(SpectatorLeftPacket(spectatorUser.id))),
            )

            getSpectatorIds(hostSessionId).forEach { otherSpecId ->
                packetBundleService.enqueue(
                    otherSpecId,
                    PacketBundle(
                        packetWriter.serialize(FellowSpectatorLeftPacket(spectatorUser.id))
                    ),
                )
            }

            channelService.findByName(channelName).onSuccess { channel ->
                channel.id?.let { cId ->
                    if (channelService.getMemberCount(cId) <= 1) {
                        channelService.leaveChannel(hostSession, channelName)
                        channelService.delete(channel)
                    }
                }
            }

            log.info("User {} stopped spectating host {}", spectatorUser.username, hostSessionId)
        }
    }

    fun handleUserDeparture(session: Session) {
        session.spectatorHostSessionId
            .toResultOr {}
            .onSuccess {
                stopSpectating(session).onFailure { error ->
                    log.debug(
                        "Could not stop spectating for user {}: {} during departure cleanup",
                        session.user?.username,
                        error,
                    )
                }
            }

        session.id
            .toResultOr {}
            .onSuccess { sessionId ->
                getSpectatorIds(sessionId).forEach { spectatorId ->
                    sessionService.findById(spectatorId).onSuccess { spectatorSession ->
                        stopSpectating(spectatorSession).onFailure { error ->
                            log.warn(
                                "Failed to eject spectator {} from host {}: {} during departure cleanup",
                                spectatorId,
                                sessionId,
                                error,
                            )
                        }
                    }
                }
            }

        log.debug("Handled user spectator departure for session {}", session.id)
    }

    @EventListener
    fun onUserLogout(event: UserLogoutEvent) {
        handleUserDeparture(event.session)
    }

    fun broadcastFrames(
        host: Session,
        packet: pe.nanamochi.banchus.packets.client.SpectateFramesPacket,
    ) {
        val hostId = host.id ?: return

        val spectatorIds = getSpectatorIds(hostId)
        if (spectatorIds.isEmpty()) return

        val data =
            packetWriter.serialize(
                pe.nanamochi.banchus.packets.server.SpectateFramesPacket(packet.replayFrameBundle)
            )

        spectatorIds.forEach { spectatorId ->
            packetBundleService.enqueue(spectatorId, PacketBundle(data))
        }
    }
}
