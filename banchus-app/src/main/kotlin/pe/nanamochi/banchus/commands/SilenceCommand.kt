package pe.nanamochi.banchus.commands

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapBoth
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.errors.*
import pe.nanamochi.banchus.infrastructure.commands.BaseCommand
import pe.nanamochi.banchus.infrastructure.commands.Command
import pe.nanamochi.banchus.service.SilenceService
import pe.nanamochi.banchus.service.UserService

@Component
@Command(name = "silence", documentation = "Silence a user for a specified duration.")
class SilenceCommand(
    private val userService: UserService,
    private val silenceService: SilenceService,
) : BaseCommand() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun processCommand(user: User, trigger: String, args: Array<String>): String {
        val targetUsername = args.getOrNull(0) ?: return "Usage: !silence <username> <duration>"
        val durationInput =
            args.drop(1).joinToString(" ").takeIf { it.isNotBlank() } ?: return "Missing duration."

        return binding {
                val target = userService.findByUsername(targetUsername).bind()
                if (target.isRestricted) Err(UserRestricted).bind()

                silenceService.silenceUser(user, target, durationInput).bind()
                target
            }
            .mapBoth(
                success = { target ->
                    log.info("Admin ${user.username} silenced ${target.username}")
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
            InvalidDuration -> "Invalid duration format (e.g., 1h30m)."
            SessionNotFound -> "User is offline."
            else -> "Internal error executing silence."
        }
}
