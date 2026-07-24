package pe.nanamochi.banchus.identity.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toResultOr
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.auth.service.SessionService
import pe.nanamochi.banchus.core.entity.AuditAction
import pe.nanamochi.banchus.core.entity.AuditLog
import pe.nanamochi.banchus.core.enums.ServerPrivileges
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.InvalidCredentials
import pe.nanamochi.banchus.core.error.UserNotFound
import pe.nanamochi.banchus.core.service.AuditLogService
import pe.nanamochi.banchus.core.util.runDatabaseCatching
import pe.nanamochi.banchus.identity.entity.User
import pe.nanamochi.banchus.identity.repository.UserRepository
import pe.nanamochi.banchus.score.service.LeaderboardService

@Service
class UserService(
    private val userRepository: UserRepository,
    private val leaderboardService: LeaderboardService,
    private val sessionService: SessionService,
    private val auditLogService: AuditLogService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun create(user: User): Result<User, DomainMessage> = runDatabaseCatching {
        userRepository.save(user)
    }

    fun update(user: User): Result<User, DomainMessage> =
        if (userRepository.existsById(user.id)) {
            runDatabaseCatching { userRepository.save(user) }
        } else {
            Err(UserNotFound)
        }

    fun fetchOneById(userId: Int): Result<User, DomainMessage> =
        userRepository.findUserById(userId).toResultOr { UserNotFound }

    fun fetchOneByUsername(username: String): Result<User, DomainMessage> =
        userRepository.findByUsername(username).toResultOr { UserNotFound }

    fun fetchOneByEmail(email: String): Result<User, DomainMessage> =
        userRepository.findByEmail(email).toResultOr { UserNotFound }

    fun login(username: String, password: String): Result<User, DomainMessage> {
        return userRepository
            .findByUsername(username)
            .toResultOr { InvalidCredentials }
            .andThen { user ->
                if (BCrypt.checkpw(password, user.passwordBcrypt)) {
                    Ok(user)
                } else {
                    Err(InvalidCredentials)
                }
            }
    }

    fun restrict(user: User, summary: String): Result<Unit, DomainMessage> = binding {
        if (user.isRestricted) return@binding

        user.privileges = user.privileges and ServerPrivileges.UNRESTRICTED.value.inv()
        user.restrictionTime = Instant.now()
        update(user).bind()

        notifyRestriction(user)
        val banchoBot = fetchOneById(1).bind()
        auditLogService
            .create(
                AuditLog(
                    admin = banchoBot,
                    target = user,
                    action = AuditAction.RESTRICT,
                    summary = summary,
                )
            )
            .bind()
        leaderboardService.removeFromAllLeaderboards(user)

        // TODO: remove first places

        log.info("User ${user.username} (${user.id}) has been restricted.")
    }

    private fun notifyRestriction(user: User) {
        sessionService.fetchByUserId(user.id).forEach { session ->
            session.privileges = user.privileges
            sessionService.update(session)
        }
    }
}
