package pe.nanamochi.banchus.infrastructure.command

import pe.nanamochi.banchus.core.enums.ServerPrivileges

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Command(
    val name: String,
    val privileges: Array<ServerPrivileges> = [ServerPrivileges.UNRESTRICTED],
    val documentation: String = "",
    val multiplayer: Boolean = false,
)
