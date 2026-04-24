package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Message
import pe.nanamochi.banchus.database.entity.MessageSendResult
import pe.nanamochi.banchus.database.entity.Target
import pe.nanamochi.banchus.database.entity.TargetInfo
import pe.nanamochi.banchus.database.repository.MessageRepository
import pe.nanamochi.banchus.domain.error.ChannelIsUnauthorized
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.domain.error.InteractionBlocked
import pe.nanamochi.banchus.domain.error.MessageInvalidLength
import pe.nanamochi.banchus.domain.error.MessageUserAutoSilenced
import pe.nanamochi.banchus.domain.error.RelationshipNotFound
import pe.nanamochi.banchus.domain.error.UserNotFound
import pe.nanamochi.banchus.domain.error.UserSilenced
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.util.runDatabaseCatching

private const val CHAT_SPAM_RATE_INTERVAL = 10
private const val CHAT_SPAM_RATE = 10
private const val CHAT_TIMEOUT_SECONDS = 5 * 60

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val userService: UserService,
    private val channelService: ChannelService,
    private val relationshipService: RelationshipService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun fetchUnreadMessages(targetId: Int): Result<List<Message>, DomainMessage> =
        runDatabaseCatching {
            messageRepository.findAllByTargetIdAndDeletedAtIsNullAndReadAtIsNull(targetId)
        }

    fun messageCount(senderId: Int, deltaSeconds: Int) = runDatabaseCatching {
        messageRepository.countBySenderIdAndCreatedAtAfter(
            senderId,
            Instant.now().minusSeconds(deltaSeconds.toLong()),
        )
    }

    fun markAllAsRead(targetId: Int): Result<Unit, DomainMessage> = runDatabaseCatching {
        messageRepository.markAllAsRead(targetId)
    }

    fun softDeleteRecent(senderId: Int, deltaSeconds: Instant) = runDatabaseCatching {
        messageRepository.softDeleteRecent(senderId, deltaSeconds)
    }

    fun checkSpam(session: Session): Result<Unit, DomainMessage> = binding {
        val messageCount = messageCount(session.userId, CHAT_SPAM_RATE_INTERVAL).bind()
        if (messageCount < CHAT_SPAM_RATE) {
            Ok(Unit)
        }

        session.silenceEnd = Instant.now().plusSeconds(CHAT_TIMEOUT_SECONDS.toLong())
        // userService.silenceUser() // TODO:

        Err(MessageUserAutoSilenced)
    }

    fun send(
        session: Session,
        target: Target,
        messageContent: String,
    ): Result<MessageSendResult, DomainMessage> = binding {
        if (session.isSilenced) {
            Err(UserSilenced)
        }

        val messageContent = messageContent.trim()
        if (messageContent.isEmpty() && messageContent.length > 500) {
            Err(MessageInvalidLength)
        }

        checkSpam(session).bind()
        val targetInfo = getTargetInfo(session, target).bind()
        val message =
            messageRepository.save(
                Message(
                    id = 0,
                    senderId = session.userId,
                    senderName = session.username,
                    targetChannel = targetInfo.targetChannel!!.resolve(),
                    targetId = targetInfo.targetId,
                    content = messageContent,
                    readAt = Instant.now().takeIf { !targetInfo.markAsUnread },
                    deletedAt = null,
                )
            )

        // val response = commands.tryHandleCommand(session, messageContent, target) // TODO:
        // Implement command handling
        MessageSendResult(
            message = message,
            response = "", // TODO: Implement command handling
        )
    }

    fun getTargetInfo(sender: Session, target: Target): Result<TargetInfo, DomainMessage> =
        binding {
            when (target) {
                is Target.Channel -> {
                    val channel = channelService.fetchOne(target.channelName).bind()

                    if (!channel.canWrite(sender.privileges)) {
                        Err(ChannelIsUnauthorized).bind<TargetInfo>()
                    }

                    TargetInfo(targetChannel = target.channelName)
                }

                is Target.UserSessions -> {
                    val receiver = target.sessions.firstOrNull() ?: Err(UserNotFound).bind()

                    if (receiver.isRestricted) {
                        Err(InteractionBlocked).bind<TargetInfo>()
                    }

                    if (receiver.privateDms) {
                        relationshipService.fetchOne(receiver.userId, sender.userId).onFailure {
                            error ->
                            if (error is RelationshipNotFound) {
                                Err(InteractionBlocked).bind<TargetInfo>()
                            }
                            log.error("Error fetching relationship.")
                        }
                    }

                    TargetInfo(targetId = receiver.userId)
                }

                is Target.OfflineUser -> {
                    val user = userService.fetchOneByUsername(target.username).bind()

                    if (user.isRestricted) {
                        Err(InteractionBlocked).bind<TargetInfo>()
                    }

                    TargetInfo(targetId = user.id, markAsUnread = true)
                }

                is Target.Bot -> {
                    TargetInfo(targetId = 1)
                }
            }
        }
}
