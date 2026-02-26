package pe.nanamochi.banchus.commands

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.infrastructure.commands.BaseCommand
import pe.nanamochi.banchus.infrastructure.commands.Command

@Component
@Command(name = "help", documentation = "Displays a list of available commands.")
class HelpCommand(
    private val commands: List<BaseCommand>,
    @Value($$"${banchus.command-prefix:!}") private val prefix: String,
) : BaseCommand() {
    override fun processCommand(user: User, trigger: String, args: Array<String>): String {
        return commands.joinToString("\n") { cmd ->
            "$prefix${cmd.config.name} - ${cmd.config.documentation}"
        }
    }
}
