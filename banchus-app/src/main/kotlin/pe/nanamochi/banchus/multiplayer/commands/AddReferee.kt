package pe.nanamochi.banchus.multiplayer.commands

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.identity.service.UserService
import pe.nanamochi.banchus.infrastructure.command.BaseCommand
import pe.nanamochi.banchus.infrastructure.command.Command
import pe.nanamochi.banchus.multiplayer.service.MultiplayerService

@Component
@Command(name = "addref", documentation = "Add a referee to the current match.", multiplayer = true)
class AddReferee(
    private val multiplayerService: MultiplayerService,
    private val userService: UserService,
) : BaseCommand() {
    override fun processCommand(session: Session, trigger: String, args: Array<String>): String {
        val targetUsername = args.getOrNull(0) ?: return "Usage: !referee <username>"
        return binding {
                val matchId =
                    multiplayerService
                        .fetchSessionMatchId(session.sessionId)
                        .toResultOr { "You must be in a match to use this command." }
                        .bind()

                val targetUser =
                    userService
                        .fetchOneByUsername(targetUsername)
                        .mapError { "User not found: $targetUsername" }
                        .bind()

                multiplayerService.addReferee(matchId, targetUser.id)

                "${targetUser.username} has been added as a referee."
            }
            .get() ?: "An unknown error occurred."
    }
}
