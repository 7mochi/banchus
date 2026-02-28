package pe.nanamochi.banchus.infrastructure.commands

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.database.entity.Channel
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.errors.ChannelNotFound
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.InternalError

@Component
class CommandProcessor(private val commands: List<BaseCommand>) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(
        prefix: String,
        message: String,
        user: User,
        channel: Channel?,
    ): Result<String, DomainMessage> {
        val parts =
            message.trim().split(Regex("\\s+")).takeIf { it.isNotEmpty() }
                ?: return Err(InternalError)

        val trigger = parts[0]
        val isMultiplayer = channel?.name?.startsWith("#mp_") ?: false

        val command =
            commands.find { it.shouldExecute(prefix, message, user.privileges, isMultiplayer) }
                ?: return Err(ChannelNotFound)

        val args = parts.drop(1).toTypedArray()

        return runCatching { command.processCommand(user, trigger, args) }
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
