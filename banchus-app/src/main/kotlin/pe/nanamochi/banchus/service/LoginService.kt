package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import java.net.InetAddress
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.components.UserPresence
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.enums.ServerPrivileges
import pe.nanamochi.banchus.domain.errors.DatabaseError
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.InternalError
import pe.nanamochi.banchus.domain.errors.InvalidCredentials
import pe.nanamochi.banchus.domain.errors.InvalidLoginFormat
import pe.nanamochi.banchus.domain.errors.SessionNotFound
import pe.nanamochi.banchus.domain.errors.StatNotFound
import pe.nanamochi.banchus.domain.errors.UserNotFound
import pe.nanamochi.banchus.dto.Geolocation
import pe.nanamochi.banchus.dto.LoginData
import pe.nanamochi.banchus.dto.LoginResponse
import pe.nanamochi.banchus.infrastructure.clients.IPApiClient
import pe.nanamochi.banchus.packets.server.AccountRestrictedPacket
import pe.nanamochi.banchus.packets.server.AnnouncePacket
import pe.nanamochi.banchus.packets.server.ChannelAvailablePacket
import pe.nanamochi.banchus.packets.server.ChannelInfoCompletePacket
import pe.nanamochi.banchus.packets.server.LoginPermissionsPacket
import pe.nanamochi.banchus.packets.server.LoginReplyPacket
import pe.nanamochi.banchus.packets.server.MessagePacket
import pe.nanamochi.banchus.packets.server.ProtocolNegotiationPacket
import pe.nanamochi.banchus.packets.server.SilenceInfoPacket
import pe.nanamochi.banchus.packets.server.UserPresencePacket
import pe.nanamochi.banchus.packets.server.UserStatsPacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.PacketBundle
import pe.nanamochi.banchus.util.toClientPrivileges

@Service
class LoginService(
    private val userService: UserService,
    private val sessionService: SessionService,
    private val statService: StatService,
    private val rankingService: RankingService,
    private val channelService: ChannelService,
    private val packetBundleService: PacketBundleService,
    private val ipApiService: IPApiClient,
    private val packetWriter: PacketWriter,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handleLogin(rawData: String, headers: HttpHeaders): LoginResponse {
        return binding {
                val loginData = parseLoginData(rawData).bind()
                val ipRaw = headers.getFirst("X-Real-IP").toResultOr { InternalError }.bind()
                val ipAddress =
                    runCatching { InetAddress.getByName(ipRaw) }.mapError { InternalError }.bind()
                val user = userService.login(loginData.username, loginData.passwordMd5).bind()

                processSuccessfulLogin(user, loginData, ipAddress).bind()
            }
            .onSuccess {
                log.info(
                    "User '{}' logged in successfully from IP {}",
                    it.token,
                    headers.getFirst("X-Real-IP"),
                )
            }
            .getOrElse { domainError ->
                val message =
                    when (domainError) {
                        is InvalidLoginFormat -> "Invalid bancho login format."
                        is InvalidCredentials -> "Invalid username or password."
                        is StatNotFound -> "Stats not found."
                        is SessionNotFound -> "Session not found."
                        is DatabaseError -> "Database connection error."
                        else -> "An internal error occurred."
                    }
                buildErrorResponse(message)
            }
    }

    private fun processSuccessfulLogin(
        user: User,
        loginData: LoginData,
        ip: InetAddress,
    ): Result<LoginResponse, DomainMessage> {
        return binding {
            val geolocation =
                if (ip.isLoopbackAddress) Geolocation.local() else ipApiService.fetchFromIP(ip)
            val ownSession =
                sessionService
                    .create(
                        Session(
                            user = user,
                            utcOffset = loginData.utcOffset,
                            gamemode = Mode.OSU,
                            country =
                                CountryCode.fromCode(geolocation.countryCode?.lowercase() ?: "xx"),
                            latitude = geolocation.latitude,
                            longitude = geolocation.longitude,
                            displayCityLocation = loginData.displayCity,
                            pmPrivate = loginData.pmPrivate,
                            primarySession = !loginData.osuVersion.contains("tourney"),
                            osuVersion = loginData.osuVersion,
                            osuPathMd5 = loginData.osuPathMd5,
                            adaptersStr = loginData.adaptersStr,
                            adaptersMd5 = loginData.adaptersMd5,
                            uninstallMd5 = loginData.uninstallMd5,
                            diskSignatureMd5 = loginData.diskSignatureMd5,
                            lastCommunicatedAt = Instant.now(),
                        )
                    )
                    .bind()
            val ownStats = statService.findByUserAndGamemode(user, ownSession.gamemode).bind()
            val ownGlobalRank = rankingService.getGlobalRank(ownSession.gamemode, user).toInt()

            sendPresenceToOtherUsers(ownSession, ownStats, ownGlobalRank)

            val loginFlowPackets = buildList {
                addAll(getLoginSuccessPacketList(user, ownSession, ownStats, ownGlobalRank).bind())
                addAll(getOtherUsersPresencePacketList(ownSession))
                addAll(getWelcomeAndStatusPacketList(user))
            }

            val finalPayload = packetWriter.serializeAll(loginFlowPackets)
            LoginResponse(token = ownSession.id.toString(), payload = finalPayload, success = true)
        }
    }

    private fun sendPresenceToOtherUsers(ownSession: Session, ownStats: Stat, ownGlobalRank: Int) {
        if (ownSession.user?.isRestricted == true) return

        mapToPacketUser(ownSession, ownStats, ownGlobalRank).onSuccess { packetUser ->
            val presence =
                packetWriter.serializeAll(
                    listOf(UserPresencePacket(packetUser), UserStatsPacket(packetUser))
                )
            val packetBundle = PacketBundle(presence)

            sessionService
                .findAll()
                .filter { it.id != ownSession.id }
                .forEach { other ->
                    other.id?.let { packetBundleService.enqueue(it, packetBundle) }
                }
        }
    }

    private fun getLoginSuccessPacketList(
        user: User,
        session: Session,
        stats: Stat,
        globalRank: Int,
    ): Result<List<BanchoPacket.Server>, DomainMessage> {
        return binding {
            val packetUser = mapToPacketUser(session, stats, globalRank).bind()

            buildList {
                val effectivePrivileges = user.privileges or ServerPrivileges.SUPPORTER.value

                add(ProtocolNegotiationPacket())
                add(LoginReplyPacket(user.id))
                add(LoginPermissionsPacket(effectivePrivileges.toClientPrivileges()))

                channelService
                    .findByAutoJoin(true)
                    .filter { c ->
                        channelService.canRead(c, user.privileges) && c.name != "#lobby"
                    }
                    .forEach { c ->
                        c.id?.let { id ->
                            val memberCount = channelService.getMemberIds(id).size
                            add(ChannelAvailablePacket(c.name, c.topic, memberCount))
                        }
                    }

                add(ChannelInfoCompletePacket())
                add(UserPresencePacket(packetUser))
                add(UserStatsPacket(packetUser))
            }
        }
    }

    private fun getOtherUsersPresencePacketList(ownSession: Session): List<BanchoPacket.Server> {
        return buildList {
            sessionService
                .findAll()
                .filter { it.id != ownSession.id && it.user?.isRestricted == false }
                .forEach { otherSession ->
                    val dbUser = otherSession.user ?: return@forEach
                    val packetUser =
                        statService
                            .findByUserAndGamemode(dbUser, otherSession.gamemode)
                            .andThen { stats ->
                                val rank =
                                    rankingService
                                        .getGlobalRank(otherSession.gamemode, dbUser)
                                        .toInt()
                                mapToPacketUser(otherSession, stats, rank)
                            }
                            .get() ?: return@forEach

                    add(UserPresencePacket(packetUser))
                    add(UserStatsPacket(packetUser))
                }
        }
    }

    private fun getWelcomeAndStatusPacketList(user: User): List<BanchoPacket.Server> {
        return buildList {
            add(AnnouncePacket("Welcome to Banchus!"))

            user.silenceEnd?.let { end ->
                val secondsRemaining = java.time.Duration.between(Instant.now(), end).toSeconds()

                if (secondsRemaining > 0) {
                    add(SilenceInfoPacket(secondsRemaining.toInt()))
                } else {
                    user.silenceEnd = null
                    userService.update(user)
                }
            }

            if (user.isRestricted) {
                add(AccountRestrictedPacket())
                add(
                    MessagePacket(
                        target = user.username,
                        sender = "BanchoBot",
                        content = "Your account is currently in restricted mode.",
                        senderId = 1,
                    )
                )
            }
        }
    }

    private fun mapToPacketUser(
        session: Session,
        stats: Stat,
        globalRank: Int,
    ): Result<pe.nanamochi.banchus.components.User, DomainMessage> {
        val dbUser = session.user ?: return Err(UserNotFound)

        return Ok(
            pe.nanamochi.banchus.components.User(
                id = dbUser.id,
                username = dbUser.username,
                globalRank = globalRank,
                presence =
                    UserPresence(
                        utcOffset = (session.utcOffset + 24).toUByte(),
                        country = session.country.id.toUByte(),
                        permissions = dbUser.privileges.toClientPrivileges().toUByte(),
                        latitude = session.latitude,
                        longitude = session.longitude,
                    ),
                stats =
                    pe.nanamochi.banchus.components.UserStats(
                        globalRank = globalRank.toUInt(),
                        rankedScore = stats.rankedScore.toULong(),
                        totalScore = stats.totalScore.toULong(),
                        accuracy = stats.accuracy.toFloat(),
                        playCount = stats.playCount.toUInt(),
                        performancePoints = stats.performancePoints.toUShort(),
                    ),
                status =
                    pe.nanamochi.banchus.components.UserStatus(
                        action = session.action.toUByte(),
                        infoText = session.infoText,
                        beatmapMd5 = session.beatmapMd5,
                        mods =
                            pe.nanamochi.banchus.components.Mods.fromBitmask(session.mods.toUInt()),
                        gamemode =
                            pe.nanamochi.banchus.components.Mode.fromValue(session.gamemode.value),
                    ),
            )
        )
    }

    private fun buildErrorResponse(message: String): LoginResponse {
        val errorPackets =
            packetWriter.serializeAll(listOf(LoginReplyPacket(-1), AnnouncePacket(message)))

        return LoginResponse(token = "no", payload = errorPackets, success = false)
    }

    private fun parseLoginData(data: String): Result<LoginData, InvalidLoginFormat> =
        runCatching {
                val lines = data.split("\n", limit = 3)
                val clientInfo = lines[2].split("|", limit = 5)
                val hashes = clientInfo[3].split(":", limit = 5)

                LoginData(
                    username = lines[0],
                    passwordMd5 = lines[1],
                    osuVersion = clientInfo[0],
                    utcOffset = clientInfo[1].toInt(),
                    displayCity = clientInfo[2] == "1",
                    pmPrivate = clientInfo[4] == "1",
                    osuPathMd5 = hashes[0],
                    adaptersStr = hashes[1],
                    adaptersMd5 = hashes[2],
                    uninstallMd5 = hashes[3],
                    diskSignatureMd5 = hashes[4],
                )
            }
            .mapError { InvalidLoginFormat }
}
