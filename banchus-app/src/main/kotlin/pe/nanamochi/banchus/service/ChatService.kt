package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onSuccess
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.UserNotFound
import pe.nanamochi.banchus.domain.errors.UserSilenced
import pe.nanamochi.banchus.infrastructure.commands.CommandProcessor

@Service
class ChatService(
    @Value($$"${banchus.command-prefix:!}") private val commandPrefix: String,
    private val channelService: ChannelService,
    private val commandProcessor: CommandProcessor,
) {
    fun handleIncomingMessage(
        session: Session,
        targetName: String,
        content: String,
    ): Result<Unit, DomainMessage> = binding {
        val user = session.user ?: Err(UserNotFound).bind()

        if (user.isSilenced) {
            Err(UserSilenced).bind()
        }

        val realChannelName = channelService.resolveRealChannelName(targetName, session).bind()

        if (content.startsWith(commandPrefix)) {
            processChatCommand(session, targetName, realChannelName, content)
        } else {
            channelService.broadcastMessage(session, targetName, content).bind()
        }
    }

    private fun processChatCommand(
        session: Session,
        targetName: String,
        realChannelName: String,
        content: String,
    ) {
        val channel = channelService.findByName(realChannelName).get()
        val user = session.user ?: return

        commandProcessor.handle(commandPrefix, content, user, channel).onSuccess { result ->
            val recipients =
                if (content.startsWith("${commandPrefix}help")) {
                    setOf(session.id!!)
                } else {
                    channel?.id?.let { channelService.getMemberIds(it) } ?: setOf(session.id!!)
                }
            channelService.sendBanchoBotMessage(targetName, result, recipients)
        }
    }
}
