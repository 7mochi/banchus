package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.toResultOr
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.components.Match
import pe.nanamochi.banchus.components.MatchSlot
import pe.nanamochi.banchus.components.MatchTeamType
import pe.nanamochi.banchus.components.MatchType
import pe.nanamochi.banchus.components.Mode
import pe.nanamochi.banchus.components.ScoringType
import pe.nanamochi.banchus.components.SlotTeam
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.database.entity.Channel
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.domain.enums.MatchStatus
import pe.nanamochi.banchus.domain.enums.Mods
import pe.nanamochi.banchus.domain.enums.ServerPrivileges
import pe.nanamochi.banchus.domain.enums.SlotStatus
import pe.nanamochi.banchus.domain.errors.ChangeSlotNotAllowed
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.IncorrectPassword
import pe.nanamochi.banchus.domain.errors.MatchNotFound
import pe.nanamochi.banchus.domain.errors.MultiplayerError
import pe.nanamochi.banchus.domain.errors.NotHost
import pe.nanamochi.banchus.domain.errors.NotInMatch
import pe.nanamochi.banchus.domain.errors.SessionNotFound
import pe.nanamochi.banchus.domain.errors.SlotNotAvailable
import pe.nanamochi.banchus.domain.errors.SlotNotFound
import pe.nanamochi.banchus.domain.errors.UserNotFound
import pe.nanamochi.banchus.domain.errors.UserSilenced
import pe.nanamochi.banchus.events.UserLogoutEvent
import pe.nanamochi.banchus.packets.client.MatchChangeSettingsPacket
import pe.nanamochi.banchus.packets.client.MatchChangeSlotPacket
import pe.nanamochi.banchus.packets.client.MatchCreatePacket
import pe.nanamochi.banchus.packets.client.MatchJoinPacket
import pe.nanamochi.banchus.packets.client.MatchLockPacket
import pe.nanamochi.banchus.packets.client.MatchScoreUpdatePacket
import pe.nanamochi.banchus.packets.server.AnnouncePacket
import pe.nanamochi.banchus.packets.server.ChannelRevokedPacket
import pe.nanamochi.banchus.packets.server.MatchAllPlayersLoadedPacket
import pe.nanamochi.banchus.packets.server.MatchCompletePacket
import pe.nanamochi.banchus.packets.server.MatchDisbandPacket
import pe.nanamochi.banchus.packets.server.MatchJoinFailPacket
import pe.nanamochi.banchus.packets.server.MatchJoinSuccessPacket
import pe.nanamochi.banchus.packets.server.MatchPlayerFailedPacket
import pe.nanamochi.banchus.packets.server.MatchPlayerSkippedPacket
import pe.nanamochi.banchus.packets.server.MatchSkipPacket
import pe.nanamochi.banchus.packets.server.MatchStartPacket
import pe.nanamochi.banchus.packets.server.MatchTransferHostPacket
import pe.nanamochi.banchus.packets.server.MatchUpdatePacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.MultiplayerMatch
import pe.nanamochi.banchus.redis.entity.MultiplayerSlot
import pe.nanamochi.banchus.redis.entity.PacketBundle
import pe.nanamochi.banchus.redis.repository.MultiplayerRepository

@Service
class MultiplayerService(
    private val multiplayerRepository: MultiplayerRepository,
    private val channelService: ChannelService,
    private val sessionService: SessionService,
    private val packetWriter: PacketWriter,
    private val packetBundleService: PacketBundleService,
    private val spectatorService: SpectatorService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // TODO: remove ? because is hard to fail, because its using .set() of redis
    fun create(match: MultiplayerMatch): MultiplayerMatch? {
        val matchId = multiplayerRepository.nextMatchId().toInt()

        val defaultSlots =
            MutableList(16) { index ->
                MultiplayerSlot(
                    slotId = index,
                    userId = -1,
                    status = SlotStatus.OPEN.value.toByte(),
                    team = pe.nanamochi.banchus.domain.enums.SlotTeam.NEUTRAL,
                    mods = Mods.NO_MOD.value,
                )
            }

        return match
            .apply {
                this.matchId = matchId
                this.slots = defaultSlots
            }
            .let { multiplayerRepository.create(it) }
    }

    fun findById(id: Int): Result<MultiplayerMatch, MatchNotFound> =
        multiplayerRepository.findById(id).toResultOr { MatchNotFound }

    fun findSlotBySessionId(
        matchId: Int,
        sessionId: UUID,
    ): Result<MultiplayerSlot, MultiplayerError> {
        return findById(matchId).andThen { match ->
            match.slots.find { it.sessionId == sessionId }.toResultOr { SlotNotFound }
        }
    }

    fun findSlotById(matchId: Int, slotId: Int): Result<MultiplayerSlot, MultiplayerError> {
        return findById(matchId).andThen { match ->
            match.slots.find { it.slotId == slotId }.toResultOr { SlotNotFound }
        }
    }

    fun fetchAll(): List<MultiplayerMatch> = multiplayerRepository.fetchAll()

    fun fetchAllSlots(matchId: Int): List<MultiplayerSlot> =
        multiplayerRepository.fetchAllSlots(matchId)

    fun update(match: MultiplayerMatch) = multiplayerRepository.update(match)

    fun delete(matchId: Int) = multiplayerRepository.delete(matchId)

    fun allPlayersLoaded(matchId: Int): Result<Boolean, MultiplayerError> {
        return findById(matchId).map { it.allLoaded() }
    }

    fun allPlayersSkipped(matchId: Int): Result<Boolean, MultiplayerError> {
        return findById(matchId).map { it.allSkipped() }
    }

    fun allPlayersCompleted(matchId: Int): Result<Boolean, MultiplayerError> {
        return findById(matchId).map { it.allCompleted() }
    }

    fun updateSlot(matchId: Int, updatedSlot: MultiplayerSlot): Result<Unit, DomainMessage> {
        return binding {
            val match = findById(matchId).bind()

            val index =
                match.slots.indexOfFirst { it.slotId == updatedSlot.slotId }.takeIf { it != -1 }
                    ?: Err(SlotNotAvailable).bind()

            match.slots[index] = updatedSlot
            multiplayerRepository.update(match)
        }
    }

    fun sendCurrentMatches(session: Session): Result<Unit, DomainMessage> {
        return session.id
            .toResultOr { SessionNotFound }
            .map { sessionId ->
                fetchAll().forEach { matchEntity ->
                    val matchData = matchEntity.toMatch()
                    val packet = MatchUpdatePacket(matchData)
                    val payload = packetWriter.serializeAll(listOf(packet))

                    packetBundleService.enqueue(sessionId, PacketBundle(payload))
                }
                log.info("Sent current matches to user {}", session.user?.username)
            }
    }

    fun createMatchFromCreatePacket(
        session: Session,
        packet: MatchCreatePacket,
    ): Result<Unit, DomainMessage> {
        return binding {
            val user = session.user ?: Err(UserNotFound).bind()
            user.isSilenced.toResultOr { Err(UserSilenced).bind() }
            if (session.spectatorHostSessionId != null) {
                spectatorService.stopSpectating(session).bind()
            }

            val savedMatch =
                create(packet.match.toMultiplayerMatch()).toResultOr { MatchNotFound }.bind()
            val multiplayerChannel =
                Channel(
                    name = "#mp_${savedMatch.matchId}",
                    topic = "Multiplayer match ${savedMatch.matchId}",
                    readPrivileges = ServerPrivileges.UNRESTRICTED.value,
                    writePrivileges = ServerPrivileges.UNRESTRICTED.value,
                    autoJoin = false,
                    temporary = true,
                )
            channelService.create(multiplayerChannel).bind()

            // Try to occupy the first slot (Usually ID 0)
            val claimedSlot = claimFirstAvailableSlotId(savedMatch.matchId).bind()
            joinMatchAndSlot(session, savedMatch, claimedSlot)

            log.info(
                "User {} created match {} ({})",
                session.user?.username,
                savedMatch.matchId,
                savedMatch.matchName,
            )
        }
    }

    fun claimFirstAvailableSlotId(matchId: Int): Result<Int, DomainMessage> {
        return findById(matchId).andThen { match ->
            match.slots
                .find { it.userId == -1 && it.status.toInt() == SlotStatus.OPEN.value }
                ?.let { Ok(it.slotId) } ?: Err(SlotNotAvailable)
        }
    }

    fun joinMatchFromJoinPacket(
        session: Session,
        packet: MatchJoinPacket,
    ): Result<Unit, DomainMessage> {
        return binding {
            val user = session.user ?: Err(UserNotFound).bind()
            if (user.isSilenced) Err(UserSilenced).bind()

            val match = findById(packet.matchId).bind()

            if (
                !match.matchPassword.isNullOrBlank() && match.matchPassword != packet.matchPassword
            ) {
                Err(IncorrectPassword).bind()
            }

            val slotId = claimFirstAvailableSlotId(match.matchId).bind()
            joinMatchAndSlot(session, match, slotId).bind()

            log.info(
                "User {} joined match {} ({})",
                session.user?.username,
                match.matchId,
                match.matchName,
            )
        }
    }

    private fun joinMatchAndSlot(
        session: Session,
        match: MultiplayerMatch,
        slotId: Int,
    ): Result<Unit, DomainMessage> {
        return binding {
            val user = session.user ?: Err(UserNotFound).bind()
            val sessionId = session.id ?: Err(SessionNotFound).bind()

            val slot = findSlotById(match.matchId, slotId).bind()

            // Update slot with user and session info, set status to NOT_READY
            slot.apply {
                userId = user.id
                this.sessionId = sessionId
                status = SlotStatus.NOT_READY.value.toByte()
            }
            updateSlot(match.matchId, slot).bind()

            // Join the multiplayer match and update session
            session.multiplayerMatchId = match.matchId
            sessionService.update(session)

            // Join the multiplayer channel
            channelService.joinChannel(session, "#mp_${match.matchId}", true)

            // Send the match data (with password) to the user
            packetBundleService.enqueue(
                sessionId,
                PacketBundle(packetWriter.serialize(MatchJoinSuccessPacket(match.toMatch()))),
            )

            // Broadcast match updates to all players
            broadcastMatchUpdates(match.matchId, sendToLobby = true).bind()
        }
    }

    fun startMatch(session: Session): Result<Unit, DomainMessage> {
        return binding {
            val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()
            val user = session.user ?: Err(UserNotFound).bind()

            val match = findById(matchId).bind()
            if (match.hostUserId != user.id) {
                Err(NotHost).bind()
            }

            match.status = MatchStatus.PLAYING
            update(match)

            match.slots.forEach { slot ->
                if ((slot.status.toInt() and SlotStatus.CAN_START) != 0) {
                    slot.status = SlotStatus.PLAYING.value.toByte()
                    updateSlot(matchId, slot).bind()
                }
            }

            broadcastToMatch(match, MatchStartPacket(match.toMatch()), SlotStatus.PLAYING.value)
            broadcastMatchUpdates(matchId, sendToLobby = true).bind()
            broadcastToLobby(packetWriter.serialize(MatchStartPacket(match.toMatch()))).bind()

            log.info("Match {} has started by user {}.", matchId, session.user?.username)
        }
    }

    fun leaveMatch(session: Session): Result<Unit, DomainMessage> {
        return binding {
            val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()

            val match =
                multiplayerRepository
                    .findById(matchId)
                    .toResultOr {
                        session.multiplayerMatchId = -1
                        sessionService.update(session)
                        MatchNotFound
                    }
                    .bind()

            handleMatchPart(session, match).bind()
        }
    }

    private fun handleMatchPart(
        session: Session,
        match: MultiplayerMatch,
    ): Result<Unit, DomainMessage> {
        return binding {
            val user = session.user ?: Err(UserNotFound).bind()
            val sessionId = session.id ?: Err(SessionNotFound).bind()
            val matchId = match.matchId

            val slot = findSlotBySessionId(matchId, sessionId).bind()

            resetSlot(match, slot.slotId).bind()

            channelService.leaveChannel(session, "#mp_$matchId")
            if (match.hostUserId == user.id) {
                handleHostLeaving(session, match).bind()
            } else {
                broadcastMatchUpdates(matchId, sendToLobby = true).bind()
            }

            session.multiplayerMatchId = -1
            sessionService.update(session)
        }
    }

    private fun handleHostLeaving(
        session: Session,
        match: MultiplayerMatch,
    ): Result<Unit, DomainMessage> {
        return match.slots
            .find { it.userId != -1 }
            ?.let { nextHostSlot ->
                transferHost(session, match, nextHostSlot) // If the host left, pick a new host
            } ?: disbandMatch(session, match) // No one is left in the match, close it
    }

    fun handleFailMatch(session: Session): Result<Unit, DomainMessage> {
        return binding {
            val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()
            val sessionId = session.id ?: Err(SessionNotFound).bind()

            val slot = findSlotBySessionId(matchId, sessionId).bind()
            val match = findById(matchId).bind()

            broadcastToMatch(match, MatchPlayerFailedPacket(slot.slotId), SlotStatus.PLAYING.value)

            log.info("User {} failed in match {}", session.user?.username, matchId)
        }
    }

    fun handleCompleteMatch(session: Session): Result<Unit, DomainMessage> {
        return binding {
            val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()
            val sessionId = session.id ?: Err(SessionNotFound).bind()

            val currentSlot = findSlotBySessionId(matchId, sessionId).bind()
            currentSlot.status = SlotStatus.COMPLETE.value.toByte()
            updateSlot(matchId, currentSlot).bind()

            if (!allPlayersCompleted(matchId).bind()) return@binding

            val match = findById(matchId).bind()

            match.status = MatchStatus.WAITING
            update(match)

            broadcastToMatch(match, MatchCompletePacket(), SlotStatus.COMPLETE.value)

            match.slots.forEach { slot ->
                val statusInt = slot.status.toInt()
                if (
                    statusInt and SlotStatus.PLAYING.value != 0 ||
                        statusInt == SlotStatus.COMPLETE.value
                ) {
                    slot.apply {
                        status = SlotStatus.NOT_READY.value.toByte()
                        isLoaded = false
                        isSkipped = false
                    }
                    updateSlot(matchId, slot).bind()
                }
            }

            broadcastMatchUpdates(matchId, sendToLobby = true).bind()

            log.info("All players in match {} have completed the beatmap.", matchId)
        }
    }

    fun handleLoadComplete(session: Session): Result<Unit, DomainMessage> {
        return binding {
            val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()
            val sessionId = session.id ?: Err(SessionNotFound).bind()

            val slot = findSlotBySessionId(matchId, sessionId).bind()
            slot.isLoaded = true
            updateSlot(matchId, slot).bind()

            if (allPlayersLoaded(matchId).bind()) {
                val match = findById(matchId).bind()

                broadcastToMatch(match, MatchAllPlayersLoadedPacket(), SlotStatus.PLAYING.value)
            }

            log.info("All players in match {} have loaded the beatmap.", matchId)
        }
    }

    fun handleSkipRequest(session: Session): Result<Unit, DomainMessage> {
        return binding {
            val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()
            val sessionId = session.id ?: Err(SessionNotFound).bind()

            val slot = findSlotBySessionId(matchId, sessionId).bind()
            slot.isSkipped = true
            updateSlot(matchId, slot).bind()

            val match = findById(matchId).bind()

            broadcastToMatch(match, MatchPlayerSkippedPacket(slot.slotId), SlotStatus.PLAYING.value)

            if (allPlayersSkipped(matchId).bind()) {
                broadcastToMatch(match, MatchSkipPacket(), SlotStatus.PLAYING.value)
            }

            log.info("User {} requested to skip in match {}.", session.user?.username, matchId)
        }
    }

    fun handleScoreUpdated(
        session: Session,
        packet: MatchScoreUpdatePacket,
    ): Result<Unit, DomainMessage> {
        return binding {
            val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()
            val sessionId = session.id ?: Err(SessionNotFound).bind()

            val match = findById(matchId).bind()

            val slot = findSlotBySessionId(matchId, sessionId).bind()

            packet.frame.id = slot.slotId

            broadcastToMatch(
                match,
                pe.nanamochi.banchus.packets.server.MatchScoreUpdatePacket(packet.frame),
                SlotStatus.PLAYING.value,
            )
        }
    }

    private fun transferHost(
        session: Session,
        match: MultiplayerMatch,
        newHostSlot: MultiplayerSlot,
    ): Result<Unit, DomainMessage> {
        return binding {
            val matchId = match.matchId
            val newHostId = newHostSlot.userId
            val newHostSessionId = newHostSlot.sessionId ?: Err(SessionNotFound).bind()

            match.hostUserId = newHostId
            update(match)

            // Notify new host
            packetBundleService.enqueue(
                newHostSessionId,
                PacketBundle(packetWriter.serialize(MatchTransferHostPacket())),
            )

            // Broadcast updates
            broadcastMatchUpdates(matchId, sendToLobby = true).bind()

            log.info(
                "Match {} host {} has left. New host is user ID {}.",
                matchId,
                session.user?.username,
                newHostId,
            )
        }
    }

    private fun disbandMatch(
        session: Session,
        match: MultiplayerMatch,
    ): Result<Unit, DomainMessage> {
        return binding {
            val matchId = match.matchId
            val channelName = "#mp_$matchId"

            broadcastToLobby(packetWriter.serialize(MatchDisbandPacket(matchId))).bind()

            val matchChannel = channelService.findByName(channelName).bind()

            channelService.getMemberIds(matchChannel.id!!).forEach { memberSessionId ->
                val memberSession = sessionService.findById(memberSessionId).bind()
                channelService.leaveChannel(memberSession, channelName)
            }
            channelService.delete(matchChannel)

            delete(matchId)

            session.multiplayerMatchId = -1
            sessionService.update(session)

            log.info(
                "Match {} disbanded as the host {} has left and no players remain.",
                matchId,
                session.user?.username,
            )
        }
    }

    fun handleUserDeparture(session: Session): Result<Unit, DomainMessage> {
        val matchId = session.multiplayerMatchId?.takeIf { it != -1 } ?: return Ok(Unit)

        return binding {
            val match =
                findById(matchId)
                    .mapError {
                        log.debug("Match {} not found during departure cleanup.", matchId)
                        return@binding
                    }
                    .bind()

            handleMatchPart(session, match).onFailure { error ->
                log.debug(
                    "Error handling match part for user {} in match {} during departure cleanup: {}",
                    session.user?.username,
                    matchId,
                    error,
                )
            }
        }
    }

    @EventListener
    fun onUserLogout(event: UserLogoutEvent) {
        handleUserDeparture(event.session)
    }

    fun changeMods(session: Session, mods: Int): Result<Unit, DomainMessage> {
        return binding {
            val user = session.user ?: Err(UserNotFound).bind()
            val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()
            val sessionId = session.id ?: Err(SessionNotFound).bind()

            val match = findById(matchId).bind()
            val isHost = match.hostUserId == user.id

            val speedChangingMask = Mods.SPEED_CHANGING.toInt()

            // In freemod mode, split mods between match (speed-changing) and slot
            // (non-speed-changing)
            if (match.freemodsEnabled) {
                if (isHost) {
                    // Apply the speed changing mods to the match
                    match.mods = (mods and speedChangingMask).toUInt()
                    update(match)
                }

                val slot = findSlotBySessionId(matchId, sessionId).bind()

                // And apply the non-speed changing mods to the slot
                slot.mods = (mods and speedChangingMask.inv()).toUInt()
                updateSlot(matchId, slot).bind()

                // Set the session's mode if needed
                if (session.gamemode != match.mode) {
                    session.gamemode = match.mode
                    sessionService.update(session)
                }

                broadcastMatchUpdates(matchId, sendToLobby = true).bind()
            } else if (isHost) {
                // In non-freemod mode, only host can change mods and applies to all
                match.mods = mods.toUInt()
                update(match)

                // Set all sessions' mode if needed
                if (session.gamemode != match.mode) {
                    fetchAllSlots(matchId)
                        .filter { it.userId != -1 && it.sessionId != null }
                        .forEach { slot ->
                            val slotSession = sessionService.findById(slot.sessionId!!).bind()
                            slotSession.gamemode = match.mode
                            slotSession.mods = mods
                            sessionService.update(slotSession)
                        }
                }
                broadcastMatchUpdates(matchId, sendToLobby = true).bind()
            } else {
                Err(NotHost).bind()
            }

            log.info(
                "User {} changed mods to {} in match {}",
                session.user?.username,
                Mods.fromBitmask(mods.toUInt()),
                matchId,
            )
        }
    }

    fun changePassword(session: Session, password: String?): Result<Unit, DomainMessage> {
        return binding {
            val user = session.user ?: Err(UserNotFound).bind()
            val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()

            val match = findById(matchId).bind()

            // Only the host can change the password
            if (match.hostUserId != user.id) Err(NotHost).bind()

            match.matchPassword = password
            update(match)

            broadcastMatchUpdates(matchId, sendToLobby = true).bind()

            log.info(
                "User {} changed the match password in match {}",
                session.user?.username,
                matchId,
            )
        }
    }

    fun changeSettings(
        session: Session,
        packet: MatchChangeSettingsPacket,
    ): Result<Unit, DomainMessage> {
        return binding {
            val user = session.user ?: Err(UserNotFound).bind()
            val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()
            val match = findById(matchId).bind()

            // Only the host can change match settings
            if (match.hostUserId != user.id) Err(NotHost).bind()

            val newSettings = packet.match
            var needSlotUpdates = false
            val slots = fetchAllSlots(matchId)

            val teamTypeChanged = newSettings.teamType.value != match.teamType.value
            val isVersus =
                newSettings.teamType == MatchTeamType.TEAM_VS ||
                    newSettings.teamType == MatchTeamType.TAG_TEAM_VS

            // If we switch to a versus mode, split all players into teams
            if (teamTypeChanged && isVersus) {
                needSlotUpdates = true
                var teamIndex = 0
                slots.forEach { slot ->
                    if (slot.userId == -1) return@forEach

                    slot.team =
                        if (teamIndex % 2 != 0) pe.nanamochi.banchus.domain.enums.SlotTeam.BLUE
                        else pe.nanamochi.banchus.domain.enums.SlotTeam.RED
                    teamIndex++
                }
            }

            // Copy bancho behavior
            // If freemod is activated, transfer match mods to slots
            // If freemod is disabled, clear slot mods
            if (newSettings.freemodsEnabled != match.freemodsEnabled) {
                needSlotUpdates = true
                var modsToApply = 0u

                if (newSettings.freemodsEnabled) {
                    val speedMask = Mods.SPEED_CHANGING
                    modsToApply = match.mods and speedMask.inv()
                }

                slots.filter { it.userId != -1 }.forEach { it.mods = modsToApply }
            }

            // Update slots if needed
            if (needSlotUpdates) {
                slots.filter { it.userId != -1 }.forEach { updateSlot(matchId, it).bind() }
            }

            match.apply {
                matchName = newSettings.name
                matchPassword = newSettings.password
                beatmapName = newSettings.beatmapName
                beatmapId = newSettings.beatmapId
                beatmapMd5 = newSettings.beatmapMd5
                mode = pe.nanamochi.banchus.domain.enums.Mode.fromValue(newSettings.mode.value)

                // Handle mod transfer logic if freemod was just enabled
                if (newSettings.freemodsEnabled && !freemodsEnabled) {
                    val speedMask = Mods.SPEED_CHANGING
                    mods = mods and speedMask
                } else {
                    mods = newSettings.mods
                }

                scoringType =
                    pe.nanamochi.banchus.domain.enums.ScoringType.fromValue(
                        newSettings.scoringType.value
                    )
                teamType =
                    pe.nanamochi.banchus.domain.enums.MatchTeamType.fromValue(
                        newSettings.teamType.value
                    )
                freemodsEnabled = newSettings.freemodsEnabled
                randomSeed = newSettings.randomSeed.toInt()
            }

            update(match)
            broadcastMatchUpdates(matchId, sendToLobby = true).bind()

            log.info("User {} changed settings in match {}.", session.user?.username, matchId)
        }
    }

    fun changeSlot(session: Session, packet: MatchChangeSlotPacket): Result<Unit, DomainMessage> {
        return binding {
            val user = session.user ?: Err(UserNotFound).bind()
            if (user.isSilenced) Err(UserSilenced).bind()

            val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()
            val match = findById(matchId).bind()

            val currentSlot = findSlotBySessionId(matchId, session.id!!).bind()
            val targetSlot = findSlotById(matchId, packet.slotId).bind()

            if (SlotStatus.fromValue(targetSlot.status.toInt()) != SlotStatus.OPEN) {
                Err(SlotNotAvailable).bind()
            }

            transferSlotData(currentSlot, targetSlot)
            updateSlot(matchId, targetSlot).bind()
            resetSlot(match, currentSlot.slotId).bind()
            broadcastMatchUpdates(matchId, sendToLobby = true).bind()

            log.info(
                "User {} moved from slot {} to slot {} in match {}.",
                session.user?.username,
                currentSlot.slotId,
                targetSlot.slotId,
                matchId,
            )
        }
    }

    fun changeSlotStatus(session: Session, newStatus: SlotStatus): Result<Unit, DomainMessage> {
        return binding {
            val user = session.user ?: Err(UserNotFound).bind()
            if (user.isSilenced) Err(UserSilenced).bind()

            val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()
            val sessionId = session.id ?: Err(SessionNotFound).bind()

            val slot = findSlotBySessionId(matchId, sessionId).bind()
            val currentStatus = SlotStatus.fromValue(slot.status.toInt())

            val allowed =
                when (newStatus) {
                    SlotStatus.READY,
                    SlotStatus.NO_BEATMAP -> currentStatus == SlotStatus.NOT_READY
                    SlotStatus.NOT_READY -> currentStatus == SlotStatus.READY
                    else -> false
                }

            if (!allowed) {
                Err(ChangeSlotNotAllowed).bind()
            }

            slot.status = newStatus.value.toByte()
            updateSlot(matchId, slot).bind()

            broadcastMatchUpdates(matchId, sendToLobby = true).bind()

            log.info(
                "User {} changed slot status to {} in match {}.",
                session.user?.username,
                newStatus,
                matchId,
            )
        }
    }

    fun lockSlot(session: Session, packet: MatchLockPacket): Result<Unit, DomainMessage> = binding {
        val user = session.user ?: Err(UserNotFound).bind()
        val matchId = session.multiplayerMatchId ?: Err(NotInMatch).bind()
        val match = findById(matchId).bind()

        // Only the host can edit slots
        if (match.hostUserId != user.id) Err(NotHost).bind()

        val slot = findSlotById(matchId, packet.slotId).bind()

        // If the slot is occupied, kick the player
        val extraSessions =
            if (slot.userId != -1 && slot.userId != user.id) {
                val slotSession = sessionService.findPrimaryByUserId(slot.userId).bind()

                channelService.leaveChannel(slotSession, "#mp_${match.matchId}")
                packetBundleService.enqueue(
                    slotSession.id!!,
                    PacketBundle(packetWriter.serialize(ChannelRevokedPacket("#multiplayer"))),
                )

                slotSession.multiplayerMatchId = -1
                sessionService.update(slotSession)

                listOf(slotSession.id!!)
            } else if (slot.userId == user.id) {
                Err(SlotNotAvailable).bind()
            } else {
                emptyList()
            }

        // Toggle lock status
        // If currently locked, unlock; otherwise, lock
        toggleSlotLock(matchId, packet.slotId).bind()

        // Broadcast updates to all players
        broadcastMatchUpdates(matchId, sendToLobby = true, extraSessionIds = extraSessions).bind()

        log.info(
            "User {} changed lock status of slot {} in match {}.",
            session.user?.username,
            packet.slotId,
            matchId,
        )
    }

    private fun transferSlotData(from: MultiplayerSlot, to: MultiplayerSlot) {
        to.apply {
            userId = from.userId
            sessionId = from.sessionId
            status = from.status
            team = from.team
            mods = from.mods
            isLoaded = from.isLoaded
            isSkipped = from.isSkipped
        }
    }

    private fun resetSlot(match: MultiplayerMatch, slotId: Int): Result<Unit, DomainMessage> {
        val slot = match.slots.find { it.slotId == slotId } ?: return Err(SlotNotFound)

        slot.apply {
            userId = -1
            sessionId = null
            status = SlotStatus.OPEN.value.toByte()
            team = pe.nanamochi.banchus.domain.enums.SlotTeam.NEUTRAL
            mods = 0u
            isLoaded = false
            isSkipped = false
        }

        return updateSlot(match.matchId, slot)
    }

    fun toggleSlotLock(matchId: Int, slotId: Int): Result<Unit, DomainMessage> = binding {
        val match = findById(matchId).bind()
        val slot = match.slots.find { it.slotId == slotId } ?: Err(SlotNotFound).bind()

        val wasLocked = SlotStatus.fromValue(slot.status.toInt()) == SlotStatus.LOCKED

        resetSlot(match, slotId).bind()

        // Reset and lock/unlock slot
        if (!wasLocked) {
            slot.status = SlotStatus.LOCKED.value.toByte()
            updateSlot(matchId, slot).bind()
        }

        log.info(
            "Slot {} in match {} has been {} by the host.",
            slotId,
            matchId,
            if (wasLocked) "unlocked" else "locked",
        )
    }

    fun broadcastToMatch(
        match: MultiplayerMatch,
        packet: BanchoPacket.Server,
        slotStatusFlag: Int,
    ) {
        val payload = packetWriter.serialize(packet)
        val bundle = PacketBundle(payload)

        match.slots.forEach { slot ->
            if (slot.userId != -1 && (slot.status.toInt() and slotStatusFlag) != 0) {
                slot.sessionId?.let { id -> packetBundleService.enqueue(id, bundle) }
            }
        }
    }

    fun broadcastMatchUpdates(
        matchId: Int,
        sendToLobby: Boolean,
        extraSessionIds: List<UUID> = emptyList(),
    ): Result<Unit, DomainMessage> {
        return binding {
            val match = findById(matchId).bind()
            val matchData = match.toMatch()

            val packetWithPassword = MatchUpdatePacket(match = matchData, shouldSendPassword = true)
            val payloadWithPassword = packetWriter.serialize(packetWithPassword)
            val bundleWithPassword = PacketBundle(payloadWithPassword)

            extraSessionIds.forEach { id -> packetBundleService.enqueue(id, bundleWithPassword) }

            broadcastToMatch(match, packetWithPassword, SlotStatus.HAS_PLAYER)

            if (sendToLobby) {
                val lobbyPayload =
                    packetWriter.serialize(
                        MatchUpdatePacket(match = matchData, shouldSendPassword = false)
                    )
                broadcastToLobby(lobbyPayload).bind()
            }
        }
    }

    fun broadcastToLobby(data: ByteArray): Result<Unit, DomainMessage> {
        return binding {
            val lobby = channelService.findByName("#lobby").bind()
            channelService.getMemberIds(lobby.id!!).toList().forEach { id ->
                packetBundleService.enqueue(id, PacketBundle(data))
            }
        }
    }

    fun sendMatchJoinFail(session: Session, announceMessage: String?) {
        val sessionId = session.id ?: return

        packetBundleService.enqueue(
            sessionId,
            PacketBundle(packetWriter.serialize(MatchJoinFailPacket())),
        )

        announceMessage?.let { message ->
            packetBundleService.enqueue(
                sessionId,
                PacketBundle(packetWriter.serialize(AnnouncePacket(message))),
            )
        }
    }

    private fun MultiplayerMatch.toMatch(): Match {
        return Match(
            id = this.matchId,
            inProgress = this.status == MatchStatus.PLAYING,
            type = MatchType.STANDARD,
            mods = this.mods,
            name = this.matchName,
            password = this.matchPassword,
            beatmapName = this.beatmapName,
            beatmapId = this.beatmapId,
            beatmapMd5 = this.beatmapMd5,
            hostId = this.hostUserId,
            mode = Mode.fromValue(this.mode.value),
            scoringType = ScoringType.fromValue(this.scoringType.value),
            teamType = MatchTeamType.fromValue(this.teamType.value),
            freemodsEnabled = this.freemodsEnabled,
            randomSeed = this.randomSeed.toUInt(),
            slots = this.slots.map { it.toSlot(this.freemodsEnabled) },
        )
    }

    private fun MultiplayerSlot.toSlot(matchFreemods: Boolean): MatchSlot {
        return MatchSlot(
            userId = this.userId,
            status = this.status,
            team = SlotTeam.fromValue(this.team.value),
            mods = if (matchFreemods) this.mods else 0u,
        )
    }

    private fun Match.toMultiplayerMatch(): MultiplayerMatch {
        return MultiplayerMatch(
            matchName = this.name,
            matchPassword = this.password,
            beatmapName = this.beatmapName,
            beatmapId = this.beatmapId,
            beatmapMd5 = this.beatmapMd5,
            hostUserId = this.hostId,
            mode = pe.nanamochi.banchus.domain.enums.Mode.fromValue(this.mode.value),
            mods = this.mods,
            scoringType =
                pe.nanamochi.banchus.domain.enums.ScoringType.fromValue(this.scoringType.value),
            teamType =
                pe.nanamochi.banchus.domain.enums.MatchTeamType.fromValue(this.teamType.value),
            freemodsEnabled = this.freemodsEnabled,
            randomSeed = this.randomSeed.toInt(),
            status = if (this.inProgress) MatchStatus.PLAYING else MatchStatus.WAITING,
            slots =
                this.slots
                    .mapIndexed { index, matchSlot ->
                        MultiplayerSlot(
                            slotId = index,
                            userId = matchSlot.userId,
                            status = matchSlot.status,
                            team =
                                pe.nanamochi.banchus.domain.enums.SlotTeam.fromValue(
                                    matchSlot.team.value
                                ),
                            mods = matchSlot.mods,
                        )
                    }
                    .toMutableList(),
        )
    }
}
