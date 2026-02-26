package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.toResultOr
import java.util.UUID
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Channel
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.database.repository.ChannelRepository
import pe.nanamochi.banchus.domain.errors.ChannelInsufficientPrivileges
import pe.nanamochi.banchus.domain.errors.ChannelNotFound
import pe.nanamochi.banchus.domain.errors.ChannelUserAlreadyIn
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.SessionNotFound
import pe.nanamochi.banchus.domain.errors.UserNotFound
import pe.nanamochi.banchus.packets.server.ChannelAvailableAutoJoinPacket
import pe.nanamochi.banchus.packets.server.ChannelAvailablePacket
import pe.nanamochi.banchus.packets.server.ChannelJoinSuccessPacket
import pe.nanamochi.banchus.packets.server.ChannelRevokedPacket
import pe.nanamochi.banchus.packets.server.MessagePacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.PacketBundle
import pe.nanamochi.banchus.redis.repository.ChannelMembersRepository
import pe.nanamochi.banchus.util.runDatabaseCatching

@Service
class ChannelService(
    private val channelRepository: ChannelRepository,
    private val membersRepository: ChannelMembersRepository,
    private val packetBundleService: PacketBundleService,
    private val packetWriter: PacketWriter,
    private val userService: UserService,
) {
    fun findByName(name: String): Result<Channel, ChannelNotFound> =
        channelRepository.findByName(name).toResultOr { ChannelNotFound }

    fun findByAutoJoin(autoJoin: Boolean): List<Channel> =
        channelRepository.findByAutoJoin(autoJoin)

    fun create(channel: Channel): Result<Channel, DomainMessage> = runDatabaseCatching {
        channelRepository.save(channel)
    }

    fun delete(channel: Channel): Result<Unit, DomainMessage> {
        val id = channel.id ?: return Err(ChannelNotFound)

        return if (channelRepository.existsById(id)) {
            runDatabaseCatching { channelRepository.delete(channel) }
        } else {
            Err(ChannelNotFound)
        }
    }

    fun joinChannel(channel: Channel, session: Session): Result<UUID, DomainMessage> {
        val channelId = channel.id ?: return Err(ChannelNotFound)
        val sessionId = session.id ?: return Err(SessionNotFound)
        return runDatabaseCatching { membersRepository.add(channelId, sessionId) }
    }

    fun leaveChannel(channel: Channel, session: Session): Result<Unit, DomainMessage> {
        val channelId = channel.id ?: return Err(ChannelNotFound)
        val sessionId = session.id ?: return Err(SessionNotFound)

        return runDatabaseCatching { membersRepository.remove(channelId, sessionId) }
    }

    fun getMemberIds(channelId: UUID): Set<UUID> = membersRepository.getMembers(channelId)

    fun getMemberCount(channelId: UUID): Int = membersRepository.getMemberCount(channelId)

    fun canRead(channel: Channel, privileges: Int): Boolean =
        channel.readPrivileges == 0 || (privileges and channel.readPrivileges) != 0

    fun canWrite(channel: Channel, privileges: Int): Boolean =
        channel.writePrivileges == 0 || (privileges and channel.writePrivileges) != 0

    // TODO: Bancho logic
    fun joinChannel(
        session: Session,
        channelName: String,
        isAutoJoin: Boolean,
    ): Result<Unit, DomainMessage> = binding {
        val channel = findByName(channelName).bind()
        val privileges = session.user?.privileges ?: 0

        if (!canRead(channel, privileges)) {
            Err(ChannelInsufficientPrivileges).bind()
        }

        val channelId = channel.id!!
        val currentChannelMembers = getMemberIds(channelId)
        val sessionId = session.id ?: Err(SessionNotFound).bind<UUID>()

        if (currentChannelMembers.contains(sessionId)) {
            Err(ChannelUserAlreadyIn).bind<Unit>()
        }

        joinChannel(channel, session).bind()

        val clientChannelName = resolveClientChannelName(channel.name)
        val newUserCount = getMemberCount(channelId) + 1
        val topic = channel.topic

        if (isAutoJoin) {
            packetBundleService.enqueue(
                sessionId,
                PacketBundle(
                    packetWriter.serialize(
                        ChannelAvailableAutoJoinPacket(
                            realName = clientChannelName,
                            topic = topic,
                            userCount = newUserCount,
                        )
                    )
                ),
            )
        }

        packetBundleService.enqueue(
            sessionId,
            PacketBundle(packetWriter.serialize(ChannelJoinSuccessPacket(name = clientChannelName))),
        )

        val infoPacketBundle =
            PacketBundle(
                packetWriter.serialize(
                    ChannelAvailablePacket(clientChannelName, topic, newUserCount)
                )
            )

        currentChannelMembers.forEach { memberId ->
            packetBundleService.enqueue(memberId, infoPacketBundle)
        }

        if (!channel.temporary) {
            findByName("#lobby").onSuccess { lobby ->
                val lobbyId = lobby.id ?: return@onSuccess
                getMemberIds(lobbyId)
                    .filter { id -> id != sessionId && !currentChannelMembers.contains(id) }
                    .forEach { id -> packetBundleService.enqueue(id, infoPacketBundle) }
            }
        }
    }

    fun leaveChannel(session: Session, channelName: String): Result<Unit, DomainMessage> = binding {
        val channel = findByName(channelName).bind()
        val channelId = channel.id!!
        val sessionId = session.id ?: Err(SessionNotFound).bind<UUID>()

        val currentChannelMembers = getMemberIds(channelId)

        if (!currentChannelMembers.contains(sessionId)) {
            return@binding
        }

        leaveChannel(channel, session).bind()

        val clientChannelName = resolveClientChannelName(channel.name)

        packetBundleService.enqueue(
            sessionId,
            PacketBundle(
                packetWriter.serialize(ChannelRevokedPacket(channelName = clientChannelName))
            ),
        )

        val newMemberCount = maxOf(0, getMemberCount(channelId) - 1)
        val infoPacketBundle =
            PacketBundle(
                packetWriter.serialize(
                    ChannelAvailablePacket(
                        realName = clientChannelName,
                        topic = channel.topic,
                        userCount = newMemberCount,
                    )
                )
            )

        currentChannelMembers
            .filter { it != sessionId }
            .forEach { memberId -> packetBundleService.enqueue(memberId, infoPacketBundle) }
    }

    fun broadcastMessage(
        sender: Session,
        targetName: String,
        content: String,
    ): Result<Unit, DomainMessage> = binding {
        val realChannelName = resolveRealChannelName(targetName, sender).bind()
        val channel = findByName(realChannelName).bind()

        val user = sender.user ?: Err(UserNotFound).bind()

        if (!canWrite(channel, user.privileges)) {
            Err(ChannelInsufficientPrivileges).bind<Unit>()
        }

        val truncated = if (content.length > 2000) content.take(2000) + "..." else content

        val msgPacket =
            MessagePacket(
                sender = user.username,
                content = truncated,
                target = targetName,
                senderId = user.id,
            )
        val bundle = PacketBundle(packetWriter.serialize(msgPacket))

        val senderId = sender.id ?: Err(SessionNotFound).bind<UUID>()
        val channelId = channel.id!!

        getMemberIds(channelId)
            .filter { targetId -> targetId != senderId }
            .forEach { targetId -> packetBundleService.enqueue(targetId, bundle) }
    }

    private fun resolveClientChannelName(realName: String): String =
        when {
            realName.startsWith("#mp_") -> "#multiplayer"
            realName.startsWith("#spec_") -> "#spectator"
            else -> realName
        }

    fun resolveRealChannelName(
        targetName: String,
        session: Session,
    ): Result<String, ChannelNotFound> =
        when (targetName) {
            "#multiplayer" -> {
                session.multiplayerMatchId.takeIf { it != null && it != -1 }?.let { Ok("#mp_$it") }
                    ?: Err(ChannelNotFound)
            }
            "#spectator" -> {
                (session.spectatorHostSessionId ?: session.id)?.let { Ok("#spec_$it") }
                    ?: Err(ChannelNotFound)
            }
            else -> Ok(targetName)
        }

    fun sendBanchoBotMessage(targetName: String, content: String, recipients: Set<UUID>) {
        val messageBundle =
            PacketBundle(
                packetWriter.serialize(
                    MessagePacket(sender = "BanchoBot", content = content, target = targetName, 1)
                )
            )
        recipients.forEach { recipientId ->
            packetBundleService.enqueue(recipientId, messageBundle)
        }
    }
}
