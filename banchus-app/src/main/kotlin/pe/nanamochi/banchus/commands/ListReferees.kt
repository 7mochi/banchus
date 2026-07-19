package pe.nanamochi.banchus.commands

import com.github.michaelbull.result.get
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.infrastructure.command.BaseCommand
import pe.nanamochi.banchus.infrastructure.command.Command
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.MultiplayerService
import pe.nanamochi.banchus.service.UserService

@Component
@Command(
    name = "listref",
    documentation = "List all referees in the current match.",
    multiplayer = true,
)
class ListReferees(
    private val multiplayerService: MultiplayerService,
    private val userService: UserService,
) : BaseCommand() {
    override fun processCommand(session: Session, trigger: String, args: Array<String>): String? {
        val matchId = multiplayerService.fetchSessionMatchId(session.sessionId) ?: return null
        val mpMatch = multiplayerService.fetchOne(matchId) ?: return null

        val isHost = mpMatch.hostUserId == session.userId
        val isRef = multiplayerService.isReferee(matchId, session.userId)

        if (!isHost && !isRef) return null

        val refUsernames =
            multiplayerService.getReferees(matchId).mapNotNull { refId ->
                userService.fetchOneById(refId).get()?.username
            }

        return "Referees for this match: ${refUsernames.joinToString(", ")}"
    }
}
