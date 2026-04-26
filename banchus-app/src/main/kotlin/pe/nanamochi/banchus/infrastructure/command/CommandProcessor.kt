package pe.nanamochi.banchus.infrastructure.command

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.database.entity.Target
import pe.nanamochi.banchus.domain.error.ChannelNotFound
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
        val parts =
            message.trim().split(Regex("\\s+")).takeIf { it.isNotEmpty() }
                ?: return Err(InternalError)

        when (target) {
            is Target.Channel -> {
                val trigger = parts[0]
                println("Channel resolved: ${target.channelName.resolve()}")
                val isMultiplayer = target.channelName.resolve().startsWith("#multiplayer_")

                val command =
                    commands.find {
                        it.shouldExecute(prefix, message, session.privileges, isMultiplayer)
                    } ?: return Err(ChannelNotFound)

                val args = parts.drop(1).toTypedArray()

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

            else -> {
                return Err(InternalError)
            }
        }
    }
}
