package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toResultOr
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.components.MatchTeamType
import pe.nanamochi.banchus.components.Mods
import pe.nanamochi.banchus.components.SlotTeam
import pe.nanamochi.banchus.components.hasAny
import pe.nanamochi.banchus.database.entity.ChannelName
import pe.nanamochi.banchus.database.entity.MatchEvent
import pe.nanamochi.banchus.database.entity.MatchEventType
import pe.nanamochi.banchus.database.entity.MatchGame
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.enums.SlotStatus
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.domain.error.InvalidPassword
import pe.nanamochi.banchus.domain.error.MatchNotFound
import pe.nanamochi.banchus.domain.error.MultiplayerMatchFull
import pe.nanamochi.banchus.domain.error.MultiplayerUnauthorized
import pe.nanamochi.banchus.domain.error.NotInMatch
import pe.nanamochi.banchus.domain.error.SlotNotFound
import pe.nanamochi.banchus.packets.client.MatchChangeSettingsPacket
import pe.nanamochi.banchus.packets.server.AnnouncePacket
import pe.nanamochi.banchus.packets.server.MatchAllPlayersLoadedPacket
import pe.nanamochi.banchus.packets.server.MatchCompletePacket
import pe.nanamochi.banchus.packets.server.MatchDisbandPacket
import pe.nanamochi.banchus.packets.server.MatchJoinFailPacket
import pe.nanamochi.banchus.packets.server.MatchPlayerFailedPacket
import pe.nanamochi.banchus.packets.server.MatchPlayerSkippedPacket
import pe.nanamochi.banchus.packets.server.MatchSkipPacket
import pe.nanamochi.banchus.packets.server.MatchStartPacket
import pe.nanamochi.banchus.packets.server.MatchUpdatePacket
import pe.nanamochi.banchus.packets.server.MessagePacket
import pe.nanamochi.banchus.packets.server.NewMatchPacket
import pe.nanamochi.banchus.redis.entity.MultiplayerMatch
import pe.nanamochi.banchus.redis.entity.MultiplayerMatchSlot
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.entity.SessionIdentity
import pe.nanamochi.banchus.redis.repository.MultiplayerRepository
import pe.nanamochi.banchus.redis.stream.StreamName
import pe.nanamochi.banchus.util.asBancho
import pe.nanamochi.banchus.util.userPanel

@Service
class MultiplayerService(
    private val multiplayerRepository: MultiplayerRepository,
    private val matchEventService: MatchEventService,
    private val userService: UserService,
    private val streamService: StreamService,
    private val channelService: ChannelService,
    private val sessionService: SessionService,
    private val matchGameService: MatchGameService,
    private val matchService: MatchService,
    private val presenceService: PresenceService,
    private val statService: StatService,
    private val leaderboardService: LeaderboardService,
    private val chatService: ChatService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun create(
        hostSession: Session,
        name: String,
        password: String?,
        beatmapName: String,
        beatmapMd5: String,
        beatmapId: Int,
        mode: Mode,
        maxPlayerCount: Int,
    ): Result<MultiplayerMatch, DomainMessage> = binding {
        multiplayerRepository.fetchSessionMatchId(hostSession.sessionId).let { matchId ->
            leave(hostSession.identity(), matchId)
        }

        val (mpMatch, slots) =
            multiplayerRepository.create(
                hostSession.identity(),
                name,
                password,
                beatmapName,
                beatmapMd5,
                beatmapId,
                mode.value,
                maxPlayerCount,
            )

        val user = userService.fetchOneById(hostSession.userId).bind()

        matchEventService.create(
            MatchEvent(
                match = matchService.fetchOneById(mpMatch.matchId).bind(),
                eventType = MatchEventType.MATCH_CREATED,
                user = user,
            )
        )

        streamService.leave(hostSession.sessionId, StreamName.Lobby)
        streamService.join(hostSession.sessionId, StreamName.Multiplayer(mpMatch.matchId))
        channelService.join(hostSession, ChannelName.Multiplayer(mpMatch.matchId))

        streamService.broadcastMessage(StreamName.Lobby, NewMatchPacket(mpMatch.asBancho(slots)))

        mpMatch
    }

    fun update(match: MultiplayerMatch): Result<MultiplayerMatch, DomainMessage> = binding {
        fetchOne(match.matchId) ?: Err(MatchNotFound).bind()
        val updatedMatch = multiplayerRepository.update(match, false)

        val slots = fetchAllSlots(match.matchId)
        broadcastUpdate(match, slots)
        updatedMatch
    }

    fun updateAllSlots(matchId: Long, slots: List<MultiplayerMatchSlot>) =
        multiplayerRepository.updateAllSlots(matchId, slots)

    fun delete(matchId: Long): Result<Unit, DomainMessage> = binding {
        multiplayerRepository.delete(matchId)
        channelService.close(ChannelName.Multiplayer(matchId))
        streamService.clearStream(StreamName.Multiplayer(matchId))
        streamService.clearStream(StreamName.Multiplaying(matchId))
        matchEventService.create(
            MatchEvent(
                match = matchService.fetchOneById(matchId).bind(),
                eventType = MatchEventType.MATCH_DISBANDED,
                user = userService.fetchOneById(1).bind(), // BanchoBot
            )
        )
        streamService.broadcastMessage(
            StreamName.Lobby,
            MatchDisbandPacket(matchId = matchId.toInt()),
        )
    }

    fun addReferee(matchId: Long, userId: Int) = multiplayerRepository.addReferee(matchId, userId)

    fun getReferees(matchId: Long) = multiplayerRepository.getReferees(matchId)

    fun isReferee(matchId: Long, userId: Int) = multiplayerRepository.isReferee(matchId, userId)

    fun fetchSessionMatchId(sessionId: UUID) = multiplayerRepository.fetchSessionMatchId(sessionId)

    fun fetchOne(matchId: Long) = multiplayerRepository.fetchOne(matchId)

    fun fetchAll() = multiplayerRepository.fetchAll()

    fun fetchAllSlots(matchId: Long) = multiplayerRepository.fetchAllSlots(matchId)

    fun fetchAllWithSlots(): List<Pair<MultiplayerMatch, List<MultiplayerMatchSlot>>> {
        val matches = fetchAll()
        val result = mutableListOf<Pair<MultiplayerMatch, List<MultiplayerMatchSlot>>>()
        for (match in matches) {
            val slots = fetchAllSlots(match.matchId)
            if (slots.isEmpty()) {
                delete(match.matchId)
                continue
            }
            result.add(match to slots)
        }
        return result
    }

    fun fetchUserSlot(
        matchId: Long,
        userId: Int,
    ): Result<Pair<Int, MultiplayerMatchSlot>, DomainMessage> = binding {
        val slots = fetchAllSlots(matchId)

        val (index, slot) =
            slots.withIndex().find { (_, slot) -> slot.user?.userId == userId }
                ?: Err(NotInMatch).bind()

        Pair(index, slot)
    }

    fun fetchSessionSlot(
        matchId: Long,
        sessionId: UUID,
    ): Result<Pair<Int, MultiplayerMatchSlot>, DomainMessage> = binding {
        val slots = fetchAllSlots(matchId)

        val (index, slot) =
            slots.withIndex().find { (_, slot) -> slot.user?.sessionId == sessionId }
                ?: Err(NotInMatch).bind()

        Pair(index, slot)
    }

    fun join(
        session: Session,
        matchId: Long,
        password: String?,
    ): Result<Pair<MultiplayerMatch, List<MultiplayerMatchSlot>>, DomainMessage> = binding {
        if (session.isRestricted) {
            Err(MultiplayerUnauthorized).bind()
        }

        multiplayerRepository.fetchSessionMatchId(session.sessionId)?.let { oldMatchId ->
            leave(session.identity(), oldMatchId)
        }

        val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()
        if (mpMatch.password != password) {
            Err(InvalidPassword).bind()
        }

        val existingSlots = fetchAllSlots(matchId)
        existingSlots
            .find { s ->
                s.user?.let { u -> u.userId == session.userId && u.sessionId != session.sessionId }
                    ?: false
            }
            ?.let { ghostSlot ->
                val ghostSessionId = ghostSlot.user!!.sessionId

                log.warn(
                    "Evicting ghost slot for user ${ghostSlot.user!!.userId} in match $matchId"
                )

                streamService.broadcastMessage(
                    StreamName.User(ghostSessionId),
                    MatchJoinFailPacket(),
                )

                val updatedSlots =
                    existingSlots.map { slot ->
                        if (slot.user?.sessionId == ghostSessionId) {
                            slot.apply { clear() }
                        } else slot
                    }
                multiplayerRepository.updateAllSlots(matchId, updatedSlots)

                streamService.leave(ghostSessionId, StreamName.Multiplayer(matchId))
                streamService.leave(ghostSessionId, StreamName.Multiplaying(matchId))
                channelService.leave(ghostSessionId, ChannelName.Multiplayer(matchId))
            }

        streamService.leave(session.sessionId, StreamName.Lobby)
        val slotsAfterJoin =
            multiplayerRepository.join(session.identity(), matchId)
                ?: Err(MultiplayerMatchFull).bind()

        matchEventService.create(
            MatchEvent(
                match = matchService.fetchOneById(matchId).bind(),
                eventType = MatchEventType.MATCH_USER_JOINED,
                user = userService.fetchOneById(session.userId).bind(),
            )
        )
        streamService.join(session.sessionId, StreamName.Multiplayer(matchId))
        channelService.join(session, ChannelName.Multiplayer(matchId))

        broadcastUpdate(mpMatch, slotsAfterJoin)

        Pair(mpMatch, slotsAfterJoin)
    }

    fun leave(session: SessionIdentity, matchId: Long? = null): Result<Unit, DomainMessage> =
        binding {
            val matchId =
                matchId
                    ?: multiplayerRepository.fetchSessionMatchId(session.sessionId)
                    ?: return@binding

            val mpMatch = fetchOne(matchId) ?: return@binding

            val (userCount, slots) =
                multiplayerRepository.leave(session.sessionId, matchId) ?: return@binding

            matchEventService.create(
                MatchEvent(
                    match = matchService.fetchOneById(matchId).bind(),
                    eventType = MatchEventType.MATCH_USER_LEFT,
                    user = userService.fetchOneById(session.userId).bind(),
                )
            )

            streamService.leave(session.sessionId, StreamName.Multiplayer(matchId))
            streamService.leave(session.sessionId, StreamName.Multiplaying(matchId))
            channelService.leave(session.sessionId, ChannelName.Multiplayer(matchId))

            if (userCount == 0) {
                delete(matchId)
            } else {
                if (mpMatch.hostUserId == session.userId) {
                    val nextHost = slots.firstNotNullOfOrNull { it.user }

                    if (nextHost != null) {
                        mpMatch.hostUserId = nextHost.userId
                        multiplayerRepository.update(mpMatch, false)

                        matchEventService.create(
                            MatchEvent(
                                match = matchService.fetchOneById(matchId).bind(),
                                eventType = MatchEventType.MATCH_HOST_ASSIGNMENT,
                                user = userService.fetchOneById(nextHost.userId).bind(),
                            )
                        )
                    }
                }

                broadcastUpdate(mpMatch, slots)
            }
        }

    fun updateBancho(
        matchId: Long,
        packet: MatchChangeSettingsPacket,
        checkHost: Int? = null,
    ): Result<MultiplayerMatch, DomainMessage> = binding {
        val mpMatch = multiplayerRepository.fetchOne(matchId).toResultOr { MatchNotFound }.bind()
        checkHost?.let { hostId ->
            val isRef = isReferee(matchId, hostId)
            if (mpMatch.hostUserId != hostId && !isRef) {
                Err(MultiplayerUnauthorized).bind()
            }
        }

        val updateName = mpMatch.name != packet.match.name
        val updatePrivate = mpMatch.password?.isEmpty() != packet.match.password?.isEmpty()

        if (!mpMatch.password.equals(packet.match.name)) {
            mpMatch.password = packet.match.password
        }
        if (updateName) {
            mpMatch.name = packet.match.name
        }
        if (mpMatch.beatmapName != packet.match.beatmapName) {
            mpMatch.beatmapName = packet.match.beatmapName
            mpMatch.beatmapMd5 = packet.match.beatmapMd5
        }
        mpMatch.beatmapId = packet.match.beatmapId

        val matchMods = mpMatch.mods
        val slots = multiplayerRepository.fetchAllSlots(matchId).toMutableList()
        val newMode = Mode.fromValue(packet.match.mode.value)
        if (newMode.value != mpMatch.mode) {
            // Update stats for all match members when mode changes
            val userIds = slots.mapNotNull { it.user?.userId }
            updateMatchMembersPresences(userIds, newMode)
        }

        var needSlotUpdates = false
        val teamTypeChanged = mpMatch.teamType != packet.match.teamType.value.toUByte()
        val isVersus =
            packet.match.teamType == MatchTeamType.TEAM_VS ||
                packet.match.teamType == MatchTeamType.TAG_TEAM_VS
        // If we switch to a versus mode, split all players into teams
        if (teamTypeChanged && isVersus) {
            needSlotUpdates = true
            var teamIndex = 0
            slots.forEach { slot ->
                if (slot.user?.userId == -1) return@forEach

                slot.team =
                    if (teamIndex % 2 != 0) pe.nanamochi.banchus.domain.enums.SlotTeam.BLUE
                    else pe.nanamochi.banchus.domain.enums.SlotTeam.RED
                teamIndex++
            }
        }

        if (mpMatch.freemodEnabled != packet.match.freemodsEnabled) {
            needSlotUpdates = true
            mpMatch.freemodEnabled = packet.match.freemodsEnabled
            if (mpMatch.freemodEnabled) {
                val (slotMods, matchMods) = splitMods(Mods.fromBitmask(matchMods))
                mpMatch.mods = Mods.toBitmask(matchMods)
                val slotModsBitmask = Mods.toBitmask(slotMods)
                slots.forEach { slot -> slot.user?.let { slot.mods = slotModsBitmask } }
            }
        }

        mpMatch.mode = newMode.value
        mpMatch.winCondition = packet.match.scoringType.value.toUByte()
        mpMatch.teamType = packet.match.teamType.value.toUByte()
        mpMatch.randomSeed = packet.match.randomSeed.toInt()

        // Update slots if needed
        if (needSlotUpdates) {
            multiplayerRepository.updateAllSlots(matchId, slots)
        }

        val updatedMatch = multiplayerRepository.update(mpMatch, updateName || updatePrivate)
        broadcastUpdate(updatedMatch, slots)
        updatedMatch
    }

    fun transferHostToSlot(
        matchId: Long,
        slotId: Int,
        checkHost: Int?,
    ): Result<Unit, DomainMessage> = binding {
        if (slotId !in 0..15) Err(SlotNotFound).bind()
        val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()

        checkHost?.let { hostId ->
            val isReferee = isReferee(matchId, hostId)
            val isCurrentHost = mpMatch.hostUserId == hostId

            if (!isReferee && !isCurrentHost) {
                Err(MultiplayerUnauthorized).bind()
            }
        }

        val slots = fetchAllSlots(matchId)
        val newHostUserId = slots.getOrNull(slotId)?.user?.userId ?: Err(SlotNotFound).bind()

        mpMatch.hostUserId = newHostUserId
        multiplayerRepository.update(mpMatch, false)
        broadcastUpdate(mpMatch, slots)

        matchEventService.create(
            MatchEvent(
                match = matchService.fetchOneById(matchId).bind(),
                eventType = MatchEventType.MATCH_HOST_ASSIGNMENT,
                user = userService.fetchOneById(newHostUserId).bind(),
            )
        )
    }

    fun transferHostToUser(
        matchId: Long,
        userId: Int,
        checkReferee: Int?,
    ): Result<Unit, DomainMessage> = binding {
        val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()

        checkReferee?.let { refereeId ->
            val isReferee = isReferee(matchId, refereeId)
            val isCurrentHost = mpMatch.hostUserId == refereeId

            if (!isReferee && !isCurrentHost) {
                Err(MultiplayerUnauthorized).bind()
            }
        }

        val slots = fetchAllSlots(matchId)
        slots.find { it.user?.userId == userId } ?: Err(NotInMatch).bind()

        mpMatch.hostUserId = userId
        multiplayerRepository.update(mpMatch, false)
        broadcastUpdate(mpMatch, slots)

        matchEventService.create(
            MatchEvent(
                match = matchService.fetchOneById(matchId).bind(),
                eventType = MatchEventType.MATCH_HOST_ASSIGNMENT,
                user = userService.fetchOneById(userId).bind(),
            )
        )
    }

    fun clearHost(matchId: Long): Result<Unit, DomainMessage> = binding {
        val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()
        val slots = fetchAllSlots(matchId)
        mpMatch.hostUserId = 0
        multiplayerRepository.update(mpMatch, false)
        broadcastUpdate(mpMatch, slots)
    }

    fun swapSlots(matchId: Long, fromSlotId: Int, toSlotId: Int): Result<Unit, DomainMessage> =
        binding {
            val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()
            val slots = fetchAllSlots(matchId).toMutableList()

            val fromSlot = slots[fromSlotId]
            val toSlot = slots[toSlotId]

            slots[fromSlotId] = toSlot
            slots[toSlotId] = fromSlot

            multiplayerRepository.updateSlots(
                matchId,
                listOf(fromSlotId to toSlot, toSlotId to fromSlot),
            )
            broadcastUpdate(mpMatch, slots)
        }

    fun swapSessionSlots(
        matchId: Long,
        targetSlotId: Int,
        sessionId: UUID,
    ): Result<Unit, DomainMessage> = binding {
        val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()
        val slots = fetchAllSlots(matchId).toMutableList()

        val (userSlotId, userSlot) =
            slots.withIndex().find { (_, slot) -> slot.user?.sessionId == sessionId }
                ?: Err(NotInMatch).bind()

        val targetSlot = slots[targetSlotId]
        slots[targetSlotId] = userSlot
        slots[userSlotId] = targetSlot

        multiplayerRepository.updateSlots(
            matchId,
            listOf(userSlotId to targetSlot, targetSlotId to userSlot),
        )
        broadcastUpdate(mpMatch, slots)
    }

    fun setSessionSlotStatus(
        matchId: Long,
        sessionId: UUID,
        status: SlotStatus,
        checkHost: Int? = null,
    ): Result<Unit, DomainMessage> = binding {
        val (slotId, _) = fetchSessionSlot(matchId, sessionId).bind()
        setSlotStatus(matchId, slotId, status, checkHost).bind()
    }

    fun setSlotStatus(
        matchId: Long,
        slotId: Int,
        status: SlotStatus,
        checkHost: Int?,
    ): Result<Unit, DomainMessage> = binding {
        val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()

        checkHost?.let { hostId ->
            val isRef = isReferee(matchId, hostId)
            val isHost = mpMatch.hostUserId == hostId
            if (!isRef && !isHost) {
                Err(MultiplayerUnauthorized).bind()
            }
        }

        val slots = fetchAllSlots(matchId)
        val slot = slots.getOrNull(slotId) ?: Err(SlotNotFound).bind()

        val isLocking = status == SlotStatus.LOCKED
        val isCurrentlyLocked = slot.status == SlotStatus.LOCKED

        slot.user?.let { slotUser ->
            if (isLocking) {
                slot.apply { clear() }

                sessionService.fetchOne(slotUser.sessionId)?.let { session ->
                    leave(session.identity(), matchId)
                    streamService.broadcastMessage(
                        StreamName.User(session.sessionId),
                        MatchJoinFailPacket(),
                    )
                    streamService.broadcastMessage(
                        StreamName.User(session.sessionId),
                        AnnouncePacket("You have been kicked out of the match!"),
                    )
                }
            }
        }

        if (isCurrentlyLocked && isLocking) {
            slot.status = SlotStatus.NONE
        } else {
            slot.status = status
        }

        multiplayerRepository.updateSlot(matchId, slotId, slot)

        val updatedSlots = fetchAllSlots(matchId)
        broadcastUpdate(mpMatch, updatedSlots)
    }

    fun setUserTeam(matchId: Long, userId: Int, team: SlotTeam): Result<Unit, DomainMessage> =
        binding {
            val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()
            val slots = fetchAllSlots(matchId)

            val (slotId, slot) =
                slots.withIndex().find { (_, s) -> s.user?.userId == userId }
                    ?: Err(NotInMatch).bind()

            slot.team = pe.nanamochi.banchus.domain.enums.SlotTeam.fromValue(team.value.toUByte())
            multiplayerRepository.updateSlot(matchId, slotId, slot)
            val updatedSlots = fetchAllSlots(matchId)
            broadcastUpdate(mpMatch, updatedSlots)
        }

    fun switchTeams(matchId: Long, sessionId: UUID): Result<Unit, DomainMessage> = binding {
        val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()
        val slots = fetchAllSlots(matchId)

        val (slotId, slot) =
            slots.withIndex().find { (_, s) -> s.user?.sessionId == sessionId }
                ?: Err(NotInMatch).bind()

        slot.team =
            when (slot.team) {
                pe.nanamochi.banchus.domain.enums.SlotTeam.NEUTRAL ->
                    pe.nanamochi.banchus.domain.enums.SlotTeam.BLUE
                pe.nanamochi.banchus.domain.enums.SlotTeam.BLUE ->
                    pe.nanamochi.banchus.domain.enums.SlotTeam.RED
                pe.nanamochi.banchus.domain.enums.SlotTeam.RED ->
                    pe.nanamochi.banchus.domain.enums.SlotTeam.BLUE
            }
        multiplayerRepository.updateSlot(matchId, slotId, slot)

        val updatedSlots = fetchAllSlots(matchId) // TODO: is correct?
        broadcastUpdate(mpMatch, updatedSlots)
    }

    fun changeMods(
        matchId: Long,
        mods: List<Mods>,
        slotUser: SessionIdentity?,
    ): Result<Unit, DomainMessage> = binding {
        val mpMatch = multiplayerRepository.fetchOne(matchId).toResultOr { MatchNotFound }.bind()

        slotUser?.let { user ->
            val isHost = mpMatch.hostUserId == user.userId
            val isRef = isReferee(matchId, user.userId)

            if (!isHost && !mpMatch.freemodEnabled && !isRef) {
                Err(MultiplayerUnauthorized).bind<Unit>()
            }
        }

        val matchMode = Mode.fromValue(mpMatch.mode)
        val slots = multiplayerRepository.fetchAllSlots(matchId).toMutableList()

        if (mpMatch.freemodEnabled) {
            val (newSlotMods, matchMods) = splitMods(mods)
            mpMatch.mods = Mods.toBitmask(matchMods)

            slotUser?.let { user ->
                val slotIndex = slots.indexOfFirst { it.user?.sessionId == user.sessionId }
                if (slotIndex == -1) Err(NotInMatch).bind<Unit>()

                val slot = slots[slotIndex]
                val oldSlotMods = slot.mods
                slot.mods = Mods.toBitmask(newSlotMods)

                multiplayerRepository.updateSlot(matchId, slotIndex, slot)

                val affectedMods = oldSlotMods xor Mods.toBitmask(newSlotMods)
                val reloadStats = affectedMods.hasAny(Mods.RELAX.value or Mods.AUTOPILOT.value)
                if (reloadStats) {
                    updateMatchMembersPresences(listOf(user.userId), matchMode)
                }
            }
                ?: run {
                    mpMatch.mode = matchMode.value

                    val updateUsers =
                        slots.mapNotNull { slot ->
                            slot.user?.let { user ->
                                val oldSlotMods = slot.mods
                                slot.mods = Mods.toBitmask(newSlotMods)
                                val affected = oldSlotMods xor Mods.toBitmask(newSlotMods)
                                if (affected.hasAny(Mods.RELAX.value or Mods.AUTOPILOT.value))
                                    user.userId
                                else null
                            }
                        }

                    updateMatchMembersPresences(updateUsers, matchMode)
                    multiplayerRepository.updateAllSlots(matchId, slots)
                }
        } else {
            mpMatch.mods = Mods.toBitmask(mods)

            val userIds = slots.mapNotNull { it.user?.userId }
            updateMatchMembersPresences(userIds, matchMode)
        }

        multiplayerRepository.update(mpMatch, false)
        broadcastUpdate(mpMatch, slots)

        Ok(Unit)
    }

    fun splitMods(mods: List<Mods>): Pair<List<Mods>, List<Mods>> {
        val modsValue = Mods.toBitmask(mods)
        val speedModsMask = Mods.HALF_TIME.value or Mods.DOUBLE_TIME.value or Mods.NIGHTCORE.value

        val matchModsValue = modsValue and speedModsMask
        val matchMods = Mods.fromBitmask(matchModsValue)

        val slotModsValue = modsValue and matchModsValue.inv()
        val slotMods = Mods.fromBitmask(slotModsValue)

        return Pair(slotMods, matchMods)
    }

    fun invitePlayerToMatch(
        matchId: Long,
        sender: Session,
        targetUserId: Int,
    ): Result<Unit, DomainMessage> = binding {
        val mpMatch = multiplayerRepository.fetchOne(matchId).toResultOr { MatchNotFound }.bind()

        val inviteText = mpMatch.createInviteMessage()
        val targetSessions = sessionService.fetchByUserId(targetUserId)

        targetSessions.forEach { targetSession ->
            val message =
                MessagePacket(
                    sender = sender.username,
                    senderId = sender.userId,
                    content = inviteText,
                    target = targetSession.username,
                )
            // chatService.broadcastPrivateMessage(targetSession.sessionId, ircMessage)
        }

        Ok(Unit)
    }

    fun startGame(matchId: Long, checkHost: Int?): Result<Unit, DomainMessage> = binding {
        val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()
        checkHost?.let { hostId ->
            val isRef = isReferee(matchId, hostId)
            val isHost = mpMatch.hostUserId == hostId
            if (!isRef && !isHost) {
                Err(MultiplayerUnauthorized).bind()
            }
        }
        mpMatch.inProgress = true
        val slots = fetchAllSlots(matchId)
        slots.forEach { slot ->
            val status = slot.status
            slot.user?.let { slotUser ->
                if (status == SlotStatus.READY || status == SlotStatus.NOT_READY) {
                    slot.status = SlotStatus.PLAYING
                    streamService.join(slotUser.sessionId, StreamName.Multiplaying(matchId))
                } else {
                    streamService.leave(slotUser.sessionId, StreamName.Multiplaying(matchId))
                }
            }
        }

        val game =
            matchGameService
                .create(
                    MatchGame(
                        match = matchService.fetchOneById(matchId).bind(),
                        beatmapId = mpMatch.beatmapId,
                        mode = Mode.fromValue(mpMatch.mode),
                        mods = mpMatch.mods.toInt(),
                        winCondition = mpMatch.winCondition.toInt(),
                        teamType = mpMatch.teamType.toInt(),
                    )
                )
                .bind()
        matchEventService.create(
            MatchEvent(
                match = matchService.fetchOneById(matchId).bind(),
                eventType = MatchEventType.MATCH_GAME_PLAYTHROUGH,
                gameId = game.id,
                user = userService.fetchOneById(1).bind(), // BanchoBot
            )
        )
        mpMatch.lastGameId = game.id
        multiplayerRepository.update(mpMatch, false)
        multiplayerRepository.updateAllSlots(matchId, slots)

        streamService.broadcastMessage(StreamName.Lobby, MatchUpdatePacket(mpMatch.asBancho(slots)))
        streamService.broadcastMessage(
            StreamName.Multiplayer(matchId),
            MatchStartPacket(mpMatch.asBancho(slots)),
        )
    }

    fun endGame(matchId: Long): Result<Unit, DomainMessage> = binding {
        streamService.broadcastMessage(StreamName.Multiplaying(matchId), MatchCompletePacket())
        matchGameService.gameEnded(matchId)

        val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()
        val slots = fetchAllSlots(matchId)
        mpMatch.inProgress = false
        slots.forEach { slot -> slot.user?.let { slot.status = SlotStatus.NOT_READY } }

        multiplayerRepository.update(mpMatch, false)
        multiplayerRepository.updateAllSlots(matchId, slots)
        broadcastUpdate(mpMatch, slots)
    }

    fun lockMatch(matchId: Long): Result<Unit, DomainMessage> = binding {
        val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()
        val slots = fetchAllSlots(matchId)
        slots.forEach { slot ->
            if (slot.user == null) {
                slot.status = SlotStatus.LOCKED
            }
        }
        multiplayerRepository.updateAllSlots(matchId, slots)
        broadcastUpdate(mpMatch, slots)
    }

    fun unlockMatch(matchId: Long): Result<Unit, DomainMessage> = binding {
        val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()
        val slots = fetchAllSlots(matchId)
        slots.forEach { slot ->
            if (slot.status == SlotStatus.LOCKED) {
                slot.status = SlotStatus.NONE
            }
        }
        multiplayerRepository.updateAllSlots(matchId, slots)
        broadcastUpdate(mpMatch, slots)
    }

    fun resizeMatch(matchId: Long, newSize: Int): Result<Unit, DomainMessage> = binding {
        val mpMatch = fetchOne(matchId) ?: Err(MatchNotFound).bind()
        val slots = fetchAllSlots(matchId).toMutableList()

        val players = slots.filter { it.user != null }
        slots.forEachIndexed { i, slot ->
            slot.clear()
            if (i >= newSize) {
                slot.status = SlotStatus.LOCKED
            } else {
                slot.status = SlotStatus.NONE
            }
        }

        players.take(newSize).forEachIndexed { i, playerSlot -> slots[i] = playerSlot }

        multiplayerRepository.updateAllSlots(matchId, slots)
        broadcastUpdate(mpMatch, slots)
    }

    fun playerLoaded(session: Session): Result<Boolean, DomainMessage> = binding {
        val matchId =
            multiplayerRepository.fetchSessionMatchId(session.sessionId) ?: Err(NotInMatch).bind()

        val (allLoaded, _) =
            changePlayingState(
                    matchId = matchId,
                    slotSessionId = session.sessionId,
                    mutation = { it.loaded = true },
                    predicate = { it.loaded },
                )
                .bind()

        if (allLoaded) {
            streamService.broadcastMessage(
                StreamName.Multiplaying(matchId),
                MatchAllPlayersLoadedPacket(),
            )
        }

        allLoaded
    }

    fun skipRequested(session: Session): Result<Boolean, DomainMessage> = binding {
        val matchId =
            multiplayerRepository.fetchSessionMatchId(session.sessionId) ?: Err(NotInMatch).bind()

        val (allSkipped, slotId) =
            changePlayingState(
                    matchId = matchId,
                    slotSessionId = session.sessionId,
                    mutation = { it.skipped = true },
                    predicate = { it.skipped },
                )
                .bind()

        val packet = if (allSkipped) MatchSkipPacket() else MatchPlayerSkippedPacket(slotId)

        streamService.broadcastMessage(StreamName.Multiplaying(matchId), packet)

        allSkipped
    }

    fun playerFailed(session: Session): Result<Boolean, DomainMessage> = binding {
        val matchId =
            multiplayerRepository.fetchSessionMatchId(session.sessionId) ?: Err(NotInMatch).bind()

        val (allFailed, slotId) =
            changePlayingState(
                    matchId = matchId,
                    slotSessionId = session.sessionId,
                    mutation = { it.failed = true },
                    predicate = { it.failed },
                )
                .bind()

        streamService.broadcastMessage(
            StreamName.Multiplaying(matchId),
            MatchPlayerFailedPacket(slotId),
        )

        allFailed
    }

    fun playerCompleted(session: Session): Result<Boolean, DomainMessage> = binding {
        val matchId =
            multiplayerRepository.fetchSessionMatchId(session.sessionId) ?: Err(NotInMatch).bind()

        val (allCompleted, _) =
            changePlayingState(
                    matchId = matchId,
                    slotSessionId = session.sessionId,
                    mutation = { it.completed = true },
                    predicate = { it.completed },
                )
                .bind()

        if (allCompleted) {
            endGame(matchId).bind()
        }

        allCompleted
    }

    fun updateMatchMembersPresences(
        userIds: List<Int>,
        newMode: Mode,
    ): Result<Unit, DomainMessage> = binding {
        userIds.forEach { userId ->
            val presence = presenceService.fetchOne(userId) ?: return@forEach
            presence.mode = newMode.value

            val stats = statService.fetchOne(userId, newMode).bind()
            val globalRank = leaderboardService.fetchGlobalRank(userId, newMode)

            presence.rankedScore = stats.rankedScore.toULong()
            presence.totalScore = stats.totalScore.toULong()
            presence.accuracy = stats.averageAccuracy
            presence.playcount = stats.playCount.toUInt()
            presence.performancePoints = stats.performancePoints.toUInt()
            presence.globalRank = globalRank

            val updatedPresence = presenceService.update(presence)

            updatedPresence.userPanel().forEach { packet ->
                streamService.broadcastMessage(StreamName.Main, packet)
            }
        }
    }

    private fun changePlayingState(
        matchId: Long,
        slotSessionId: UUID,
        mutation: (MultiplayerMatchSlot) -> Unit,
        predicate: (MultiplayerMatchSlot) -> Boolean,
    ): Result<Pair<Boolean, Int>, DomainMessage> = binding {
        val slots = fetchAllSlots(matchId)
        var playerSlotIndex: Int? = null

        slots.withIndex().forEach { (index, slot) ->
            if (slot.user?.sessionId == slotSessionId) {
                mutation(slot)
                slot.loaded = true
                playerSlotIndex = index
            }
        }

        val finalId = playerSlotIndex ?: Err(NotInMatch).bind()

        multiplayerRepository.updateSlot(matchId, finalId, slots[finalId])

        val allCompleted =
            slots
                .filter { it.user != null && it.status == SlotStatus.PLAYING }
                .all { predicate(it) }

        allCompleted to finalId
    }

    fun broadcastUpdate(mpMatch: MultiplayerMatch, slots: List<MultiplayerMatchSlot>) {
        val matchUpdate = MatchUpdatePacket(match = mpMatch.asBancho(slots))
        streamService.broadcastMessage(StreamName.Lobby, matchUpdate)
        streamService.broadcastMessage(StreamName.Multiplayer(mpMatch.matchId), matchUpdate)
    }

    fun MultiplayerMatch.createInviteMessage(): String {
        val pwdPart = if (!password.isNullOrEmpty()) "/$password" else ""
        return "I've invited you to join my multiplayer match: [osump://$matchId$pwdPart $name]"
    }
}
