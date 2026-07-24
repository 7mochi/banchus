package pe.nanamochi.banchus.chat.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.chat.entity.MessageSendResult
import pe.nanamochi.banchus.chat.entity.Target
import pe.nanamochi.banchus.core.error.DomainMessage

@Service
class ChatService(
    private val channelService: ChannelService,
    private val messageService: MessageService,
) {
    fun sendChannelMessage(
        content: String,
        target: String,
        session: Session,
    ): Result<MessageSendResult, DomainMessage> = binding {
        val channelName = channelService.getChannelName(session, target).bind()
        val targetObj = Target.Channel(channelName)
        messageService.send(session, targetObj, content).bind()
    }
}
