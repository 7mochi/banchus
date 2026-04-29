package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import java.net.InetAddress
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.HardwareLog
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.error.ClientTooOld
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.domain.error.SessionInvalidCredentials
import pe.nanamochi.banchus.domain.error.SessionsLimitReached
import pe.nanamochi.banchus.dto.client.LoginData
import pe.nanamochi.banchus.dto.external.Geolocation
import pe.nanamochi.banchus.packets.server.UserQuitPacket
import pe.nanamochi.banchus.redis.entity.Presence
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.repository.SessionRepository
import pe.nanamochi.banchus.redis.stream.StreamName

private const val USER_SESSIONS_LIMIT = 20
private const val TOURNAMENT_STAFF_SESSIONS_LIMIT = 40

@Service
class SessionService(
    private val sessionRepository: SessionRepository,
    private val spectatorService: SpectatorService,
    private val channelService: ChannelService,
    private val streamService: StreamService,
    private val presenceService: PresenceService,
    private val userService: UserService,
    private val statService: StatService,
    private val leaderboardService: LeaderboardService,
    private val hardwareLogService: HardwareLogService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun create(
        loginData: LoginData,
        ipAddress: InetAddress,
        geolocation: Geolocation,
    ): Result<Pair<Session, Presence>, DomainMessage> {
        if (loginData.clientInfo.osuVersion.isOutdated()) {
            return Err(ClientTooOld)
        }

        return binding {
            val user =
                userService
                    .fetchOneByUsername(loginData.identifier)
                    .mapError { SessionInvalidCredentials }
                    .bind()

            hardwareLogService
                .create(
                    HardwareLog(
                        user = user,
                        adaptersMd5 = loginData.clientInfo.clientHashes.adaptersMd5,
                        uninstallMd5 = loginData.clientInfo.clientHashes.uninstallMd5,
                        diskSignatureMd5 = loginData.clientInfo.clientHashes.diskSignatureMd5,
                    )
                )
                .bind()

            // TODO: Implement check for multiaccounts

            val userSessionCount = fetchUserSessionCount(user.id)
            if (
                !user.isTournamentStaff && userSessionCount >= USER_SESSIONS_LIMIT ||
                    userSessionCount >= TOURNAMENT_STAFF_SESSIONS_LIMIT
            ) {
                Err(SessionsLimitReached).bind()
            }

            val stats = statService.fetchOne(user.id, Mode.OSU).bind()
            val rank = leaderboardService.fetchGlobalRank(user.id, Mode.OSU)

            val session =
                sessionRepository.create(
                    Session(
                        userId = user.id,
                        username = user.username,
                        privileges = user.privileges,
                        createIpAddress = ipAddress.toString(),
                        silenceEnd = user.silenceEnd,
                        privateDms = loginData.clientInfo.pmPrivate,
                    )
                )
            val presence =
                presenceService.create(
                    Presence(
                        userId = user.id,
                        username = user.username,
                        privileges = user.privileges,
                        action = 0u, // TODO: Add action enum
                        infoText = "",
                        beatmapMd5 = "",
                        beatmapId = 0,
                        mods = 0,
                        mode = Mode.OSU.value,
                        rankedScore = stats.rankedScore.toULong(),
                        totalScore = stats.totalScore.toULong(),
                        accuracy = stats.averageAccuracy,
                        playcount = stats.playCount.toUInt(),
                        performancePoints = stats.performancePoints.toUInt(),
                        globalRank = rank,
                        countryCode = CountryCode.fromCode(geolocation.countryCode),
                        latitude = geolocation.latitude,
                        longitude = geolocation.longitude,
                        utcOffset = loginData.clientInfo.utcOffset,
                    )
                )

            Pair(session, presence)
        }
    }

    fun update(session: Session) = sessionRepository.update(session)

    fun setPrivateDms(session: Session, privateDm: Boolean): Session =
        sessionRepository.setPrivateDms(session, privateDm)

    fun fetchOne(sessionId: UUID) = sessionRepository.findById(sessionId)

    fun fetchByUserId(userId: Int) = sessionRepository.fetchByUserId(userId)

    fun fetchByUsername(username: String) = sessionRepository.fetchByUsername(username)

    fun fetchUserSessionCount(userId: Int): Int = sessionRepository.fetchUserSessionCount(userId)

    fun logout(session: Session) {
        // The osu! client will often attempt to logout as soon as they login,
        // this is a quirk of the client, and we don't really want to log them out;
        // so we ignore this case if it's been < 1 second since the client's login
        val sessionAge = Duration.between(session.createdAt, Instant.now())
        if (sessionAge < Duration.ofSeconds(1)) {
            log.debug(
                "Ignoring logout attempt < 1 second after login for user {}",
                session.username,
            )
            return
        }

        channelService.leaveAll(session.sessionId)
        spectatorService.leave(session, null)
        spectatorService.close(session.sessionId)

        val remainingSessionCount =
            sessionRepository.delete(session.sessionId, session.userId, session.username)

        streamService.clearStream(StreamName.User(session.sessionId))
        streamService.leaveAll(session.sessionId)

        if (remainingSessionCount == 0L) {
            presenceService.delete(session.userId)
            streamService.broadcastMessage(StreamName.Main, UserQuitPacket(session.userId))
        }
    }
}
