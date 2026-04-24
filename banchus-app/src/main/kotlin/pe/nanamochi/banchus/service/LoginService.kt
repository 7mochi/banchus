package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onSuccess
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.database.entity.ChannelName
import pe.nanamochi.banchus.domain.enums.ServerPrivileges
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.domain.error.InvalidCredentials
import pe.nanamochi.banchus.dto.client.LoginData
import pe.nanamochi.banchus.dto.client.LoginResponse
import pe.nanamochi.banchus.packets.server.AnnouncePacket
import pe.nanamochi.banchus.packets.server.ChannelAvailablePacket
import pe.nanamochi.banchus.packets.server.ChannelInfoCompletePacket
import pe.nanamochi.banchus.packets.server.ChannelJoinSuccessPacket
import pe.nanamochi.banchus.packets.server.FriendListPacket
import pe.nanamochi.banchus.packets.server.LoginPermissionsPacket
import pe.nanamochi.banchus.packets.server.LoginReplyPacket
import pe.nanamochi.banchus.packets.server.ProtocolNegotiationPacket
import pe.nanamochi.banchus.packets.server.SilenceInfoPacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.Presence
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.stream.StreamName
import pe.nanamochi.banchus.util.toClientPrivileges
import pe.nanamochi.banchus.util.userPanel

@Service
class LoginService(
    private val userService: UserService,
    private val sessionService: SessionService,
    private val channelService: ChannelService,
    private val presenceService: PresenceService,
    private val relationshipService: RelationshipService,
    private val geolocationService: GeolocationService,
    private val packetWriter: PacketWriter,
    private val streamService: StreamService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handleLogin(rawData: String, headers: HttpHeaders): LoginResponse {
        return binding {
                val loginData = LoginData.parse(rawData).bind()
                val user = userService.login(loginData.identifier, loginData.secret).bind()
                val (ipAddress, geolocation) = geolocationService.resolve(headers)
                val (session, presence) =
                    sessionService.create(loginData, ipAddress, geolocation).bind()

                if (!session.isRestricted) {
                    presence.userPanel().forEach { packet ->
                        streamService.broadcastMessage(StreamName.Main, packet)
                    }
                }

                val friends = relationshipService.fetchFriends(user).bind()

                val responsePackets =
                    buildList {
                            add(LoginReplyPacket(session.userId))
                            add(ProtocolNegotiationPacket())
                            add(
                                LoginPermissionsPacket(
                                    (user.privileges or ServerPrivileges.SUPPORTER.value)
                                        .toClientPrivileges()
                                )
                            )
                            add(ChannelInfoCompletePacket())
                            add(AnnouncePacket("Welcome to Banchus!"))
                            add(FriendListPacket(friends.map { friend -> friend.id }))
                            addAll(presence.userPanel())
                            addAll(Presence.botPresence().userPanel())
                        }
                        .toMutableList()

                if (session.silenceLeft != 0) {
                    responsePackets.add(SilenceInfoPacket(session.silenceLeft))
                }

                streamService.join(session.sessionId, StreamName.User(session.sessionId))
                streamService.join(session.sessionId, StreamName.Main)

                responsePackets.add(joinSpecialChannel(session, "#osu").bind())
                responsePackets.add(joinSpecialChannel(session, "#announce").bind())

                if (session.isDonor) {
                    streamService.join(session.sessionId, StreamName.Donator)
                    responsePackets.add(joinSpecialChannel(session, "#plus").bind())
                }

                if (session.isStaff) {
                    streamService.join(session.sessionId, StreamName.Staff)
                    responsePackets.add(joinSpecialChannel(session, "#staff").bind())
                }

                if (session.isDeveloper) {
                    streamService.join(session.sessionId, StreamName.Developer)
                    responsePackets.add(joinSpecialChannel(session, "#devlog").bind())
                }

                val channels = channelService.fetchAll().bind()
                channels.forEach { channel ->
                    if (channel.canRead(session.privileges)) {
                        val memberCount = channelService.memberCount(ChannelName.Chat(channel.name))
                        responsePackets.add(
                            ChannelAvailablePacket(
                                realName = channel.name,
                                topic = channel.description,
                                userCount = memberCount.toInt(),
                            )
                        )
                    }
                }

                presenceService.fetchAll().forEach { presence ->
                    if (!presence.isRestricted) responsePackets.addAll(presence.userPanel())
                }

                val finalPayload = packetWriter.serializeAll(responsePackets)
                LoginResponse(
                    token = session.sessionId.toString(),
                    payload = finalPayload,
                    success = true,
                )
            }
            .onSuccess { loginResponse ->
                log.info(
                    "User '{}' logged in successfully from IP {}",
                    loginResponse.token,
                    headers.getFirst("X-Real-IP"),
                )
            }
            .getOrElse { domainError ->
                val message =
                    when (domainError) {
                        is InvalidCredentials -> "Invalid username or password."
                        else -> "An internal error occurred."
                    }
                val errorPackets =
                    packetWriter.serializeAll(listOf(LoginReplyPacket(-1), AnnouncePacket(message)))
                LoginResponse(token = "no", payload = errorPackets, success = false)
            }
    }

    private fun joinSpecialChannel(
        session: Session,
        channelName: String,
    ): Result<ServerPacket, DomainMessage> = binding {
        channelService.join(session, ChannelName.Chat(channelName)).bind()
        ChannelJoinSuccessPacket(channelName)
    }
}
