package pe.nanamochi.banchus.chat.commands

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapBoth
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.error.BotSilenceNotAllowed
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.SelfSilenceNotAllowed
import pe.nanamochi.banchus.core.error.UserNotFound
import pe.nanamochi.banchus.core.error.UserRestricted
import pe.nanamochi.banchus.core.error.UserSilenced
import pe.nanamochi.banchus.core.service.SilenceService
import pe.nanamochi.banchus.identity.service.UserService
import pe.nanamochi.banchus.infrastructure.command.BaseCommand
import pe.nanamochi.banchus.infrastructure.command.Command

@Component
@Command(name = "silence", documentation = "Silence a user for a specified duration.")
class SilenceCommand(
    private val userService: UserService,
    private val silenceService: SilenceService,
) : BaseCommand() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun processCommand(session: Session, trigger: String, args: Array<String>): String? {
        val targetUsername = args.getOrNull(0) ?: return "Usage: !silence <username> <duration>"
        val durationInput =
            args.drop(1).joinToString(" ").takeIf { it.isNotBlank() } ?: return "Missing duration."

        return binding {
                val target = userService.fetchOneByUsername(targetUsername).bind()
                if (target.isRestricted) Err(UserRestricted).bind()

                silenceService.silenceUser(session.userId, target, durationInput).bind()
                target
            }
            .mapBoth(
                success = { target ->
                    log.info("${target.username} has been silenced.")
                    "User ${target.username} has been silenced for ${silenceService.formatRemainingSilence(target.silenceEnd!!)}."
                },
                failure = { it.toMessage() },
            )
    }

    private fun DomainMessage.toMessage() =
        when (this) {
            UserNotFound,
            UserRestricted -> "Username not found."
            SelfSilenceNotAllowed -> "You cannot silence yourself."
            BotSilenceNotAllowed -> "You cannot silence the system bot."
            UserSilenced -> "This user is already silenced."
            else -> "Internal error executing silence."
        }
}
