package pe.nanamochi.banchus.infrastructure.command

import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.enums.ServerPrivileges

abstract class BaseCommand {
    val config: Command by lazy {
        this::class.annotations.find { it is Command } as? Command
            ?: throw IllegalStateException(
                "${this::class.simpleName} must be annotated with @Command"
            )
    }

    fun shouldExecute(userPrivileges: Int, isMultiplayer: Boolean): Boolean {
        if (config.privileges.isNotEmpty()) {
            val userPrivs = ServerPrivileges.fromBitmask(userPrivileges)
            val hasPermission = config.privileges.any { it in userPrivs }
            if (!hasPermission) return false
        }

        return !config.multiplayer || isMultiplayer
    }

    abstract fun processCommand(session: Session, trigger: String, args: Array<String>): String?
}
