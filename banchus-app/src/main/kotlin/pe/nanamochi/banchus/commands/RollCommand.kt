package pe.nanamochi.banchus.commands

import org.springframework.stereotype.Component
import pe.nanamochi.banchus.infrastructure.command.BaseCommand
import pe.nanamochi.banchus.infrastructure.command.Command
import pe.nanamochi.banchus.redis.entity.Session

@Component
@Command(
    name = "roll",
    documentation = "Roll a random number between 0 and a given number.",
    multiplayer = true,
)
class RollCommand : BaseCommand() {
    override fun processCommand(session: Session, trigger: String, args: Array<String>): String {
        val max = args.getOrNull(0)?.toIntOrNull()?.coerceIn(1, 32767) ?: 100
        val result = (0..max).random()

        return "${session.username} rolls $result points."
    }
}
