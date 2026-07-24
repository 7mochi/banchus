package pe.nanamochi.banchus.auth.broadcast

import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.dto.LoginResponse
import pe.nanamochi.banchus.auth.dto.LoginResult
import pe.nanamochi.banchus.chat.entity.ChannelName
import pe.nanamochi.banchus.chat.service.ChannelService
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.entity.Presence
import pe.nanamochi.banchus.core.enums.ServerPrivileges
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.InvalidCredentials
import pe.nanamochi.banchus.core.service.StreamService
import pe.nanamochi.banchus.core.util.toClientPrivileges
import pe.nanamochi.banchus.infrastructure.util.userPanel
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

@Component
class LoginBroadcaster(
    private val packetWriter: PacketWriter,
    private val streamService: StreamService,
    private val channelService: ChannelService,
) {
    fun loginSuccess(result: LoginResult): LoginResponse {
        if (!result.session.isRestricted) {
            result.presence.userPanel().forEach { packet ->
                streamService.broadcastData(StreamName.Main, packetWriter.serialize(packet))
            }
        }

        val responsePackets =
            buildList {
                    add(LoginReplyPacket(result.session.userId))
                    add(ProtocolNegotiationPacket())
                    add(
                        LoginPermissionsPacket(
                            (result.user.privileges or ServerPrivileges.SUPPORTER.value)
                                .toClientPrivileges()
                        )
                    )
                    add(ChannelInfoCompletePacket())
                    add(AnnouncePacket("Welcome to Banchus!"))
                    add(FriendListPacket(result.friends.map { it }))
                    addAll(result.presence.userPanel())
                    addAll(Presence.botPresence().userPanel())
                }
                .toMutableList()

        if (result.session.silenceLeft != 0) {
            responsePackets.add(SilenceInfoPacket(result.session.silenceLeft))
        }

        result.joinedSpecialChannels.forEach { channelName ->
            responsePackets.add(ChannelJoinSuccessPacket(channelName))
        }

        result.channels.forEach { channel ->
            if (channel.canRead(result.session.privileges)) {
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

        result.allPresences.forEach { presence ->
            if (!presence.isRestricted) responsePackets.addAll(presence.userPanel())
        }

        val finalPayload = packetWriter.serializeAll(responsePackets)
        return LoginResponse(
            token = result.session.sessionId.toString(),
            payload = finalPayload,
            success = true,
        )
    }

    fun loginFailure(error: DomainMessage): LoginResponse {
        val message =
            when (error) {
                is InvalidCredentials -> "Invalid username or password."
                else -> "An internal error occurred."
            }
        val errorPackets =
            packetWriter.serializeAll(listOf(LoginReplyPacket(-1), AnnouncePacket(message)))
        return LoginResponse(token = "no", payload = errorPackets, success = false)
    }
}
