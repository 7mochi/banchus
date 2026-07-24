package pe.nanamochi.banchus.chat.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.chat.entity.Message
import pe.nanamochi.banchus.chat.entity.MessageSendResult
import pe.nanamochi.banchus.chat.entity.Target
import pe.nanamochi.banchus.chat.entity.TargetInfo
import pe.nanamochi.banchus.chat.repository.MessageRepository
import pe.nanamochi.banchus.identity.service.RelationshipService
import pe.nanamochi.banchus.identity.service.UserService
import pe.nanamochi.banchus.infrastructure.command.CommandProcessor
import pe.nanamochi.banchus.infrastructure.config.BanchusProperties
import pe.nanamochi.banchus.core.error.ChannelIsUnauthorized
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.InteractionBlocked
import pe.nanamochi.banchus.core.error.MessageInvalidLength
import pe.nanamochi.banchus.core.error.MessageUserAutoSilenced
import pe.nanamochi.banchus.core.error.RelationshipNotFound
import pe.nanamochi.banchus.core.error.UserNotFound
import pe.nanamochi.banchus.core.error.UserSilenced
import pe.nanamochi.banchus.core.service.SilenceService
import pe.nanamochi.banchus.core.util.runDatabaseCatching

private const val CHAT_SPAM_RATE_INTERVAL = 10
private const val CHAT_SPAM_RATE = 10

@Service
class MessageService(
    private val properties: BanchusProperties,
    private val messageRepository: MessageRepository,
    private val userService: UserService,
    private val channelService: ChannelService,
    private val relationshipService: RelationshipService,
    @Lazy private val commandProcessor: CommandProcessor,
    @Lazy private val silenceService: SilenceService,
) {
    private val commandPrefix: String = properties.commandPrefix
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

    fun softDeleteRecent(senderId: Int, duration: Duration) = runDatabaseCatching {
        val cutoff = Instant.now().minus(duration)
        messageRepository.softDeleteRecent(senderId, cutoff)
    }

    fun checkSpam(session: Session): Result<Unit, DomainMessage> = binding {
        val messageCount = messageCount(session.userId, CHAT_SPAM_RATE_INTERVAL).bind()
        if (messageCount < CHAT_SPAM_RATE) {
            return@binding
        }

        val user = userService.fetchOneById(session.userId).bind()
        silenceService.autoSilence(user).bind()

        Err(MessageUserAutoSilenced)
    }

    fun send(
        session: Session,
        target: Target,
        messageContent: String,
    ): Result<MessageSendResult, DomainMessage> = binding {
        if (session.isSilenced) Err(UserSilenced).bind()

        val messageContent = messageContent.trim()
        if (messageContent.isEmpty() || messageContent.length > 500)
            Err(MessageInvalidLength).bind()

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

        val response =
            commandProcessor.handle(commandPrefix, messageContent, session, target).bind()
        MessageSendResult(message = message, response = response)
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
