package pe.nanamochi.banchus.infrastructure.command

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.database.entity.Target
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.domain.error.InternalError
import pe.nanamochi.banchus.redis.entity.Session

@Component
class CommandProcessor(private val commands: List<BaseCommand>) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(
        prefix: String,
        message: String,
        session: Session,
        target: Target,
    ): Result<String, DomainMessage> {
        val trimmedMessage = message.trim()

        if (!trimmedMessage.startsWith(prefix)) {
            return Ok("")
        }

        val parts =
            trimmedMessage.removePrefix(prefix).split(Regex("\\s+")).takeIf { it.isNotEmpty() }
                ?: return Err(InternalError)

        val trigger = parts[0].lowercase()
        val args = parts.drop(1).toTypedArray()

        return when (target) {
            is Target.Channel -> {
                val channelName = target.channelName.resolve()
                val isMultiplayer = channelName.startsWith("#multiplayer_")

                val command =
                    commands.find { cmd ->
                        val annotation =
                            cmd::class.annotations.filterIsInstance<Command>().firstOrNull()
                        annotation?.let {
                            it.name.lowercase() == trigger &&
                                cmd.shouldExecute(session.privileges, isMultiplayer)
                        } ?: false
                    } ?: return Err(InternalError)
                executeCommand(command, session, trigger, args)
            }
            else -> Ok("")
        }
    }

    private fun executeCommand(
        command: BaseCommand,
        session: Session,
        trigger: String,
        args: Array<String>,
    ): Result<String, DomainMessage> {
        return runCatching { command.processCommand(session, trigger, args) }
            .fold(
                onSuccess = { response ->
                    if (response.isNullOrBlank()) Err(InternalError) else Ok(response)
                },
                onFailure = { e ->
                    log.error("Command execution failed: $trigger", e)
                    Ok("Error: ${e.message ?: "An unknown error occurred."}")
                },
            )
    }
}
