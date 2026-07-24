package pe.nanamochi.banchus.auth.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onSuccess
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.auth.dto.LoginData
import pe.nanamochi.banchus.auth.dto.LoginResult
import pe.nanamochi.banchus.chat.entity.ChannelName
import pe.nanamochi.banchus.chat.service.ChannelService
import pe.nanamochi.banchus.identity.service.GeolocationService
import pe.nanamochi.banchus.identity.service.RelationshipService
import pe.nanamochi.banchus.identity.service.UserService
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.service.PresenceService
import pe.nanamochi.banchus.core.service.StreamService

@Service
class LoginService(
    private val userService: UserService,
    private val sessionService: SessionService,
    private val channelService: ChannelService,
    private val presenceService: PresenceService,
    private val relationshipService: RelationshipService,
    private val geolocationService: GeolocationService,
    private val streamService: StreamService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handleLogin(rawData: String, headers: HttpHeaders): Result<LoginResult, DomainMessage> {
        return binding {
                val loginData = LoginData.parse(rawData).bind()
                val user = userService.login(loginData.identifier, loginData.secret).bind()
                val (ipAddress, geolocation) = geolocationService.resolve(headers)
                val (session, presence) =
                    sessionService.create(loginData, ipAddress, geolocation).bind()

                val friends = relationshipService.fetchFriends(user).bind()

                streamService.join(session.sessionId, StreamName.User(session.sessionId))
                streamService.join(session.sessionId, StreamName.Main)

                val joinedSpecialChannels = mutableListOf<String>()

                joinedSpecialChannels.add(joinSpecialChannel(session, "#osu").bind())
                joinedSpecialChannels.add(joinSpecialChannel(session, "#announce").bind())

                if (session.isDonor) {
                    streamService.join(session.sessionId, StreamName.Donator)
                    joinedSpecialChannels.add(joinSpecialChannel(session, "#plus").bind())
                }

                if (session.isStaff) {
                    streamService.join(session.sessionId, StreamName.Staff)
                    joinedSpecialChannels.add(joinSpecialChannel(session, "#staff").bind())
                }

                if (session.isDeveloper) {
                    streamService.join(session.sessionId, StreamName.Developer)
                    joinedSpecialChannels.add(joinSpecialChannel(session, "#devlog").bind())
                }

                val channels = channelService.fetchAll().bind()

                val allPresences = presenceService.fetchAll()

                LoginResult(
                    session = session,
                    user = user,
                    friends = friends.map { it.id },
                    channels = channels,
                    presence = presence,
                    allPresences = allPresences,
                    joinedSpecialChannels = joinedSpecialChannels,
                )
            }
            .onSuccess { loginResult ->
                log.info(
                    "User '{}' logged in successfully from IP {}",
                    loginResult.user.username,
                    headers.getFirst("X-Real-IP"),
                )
            }
    }

    private fun joinSpecialChannel(
        session: pe.nanamochi.banchus.auth.entity.Session,
        channelName: String,
    ): Result<String, DomainMessage> = binding {
        channelService.join(session, ChannelName.Chat(channelName)).bind()
        channelName
    }
}
