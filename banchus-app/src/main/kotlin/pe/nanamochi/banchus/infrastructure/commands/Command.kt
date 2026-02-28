package pe.nanamochi.banchus.infrastructure.commands

import pe.nanamochi.banchus.domain.enums.ServerPrivileges

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Command(
    val name: String,
    val privileges: Array<ServerPrivileges> = [ServerPrivileges.UNRESTRICTED],
    val documentation: String = "",
    val multiplayer: Boolean = false,
)
