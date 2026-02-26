package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import java.util.UUID
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.database.repository.SessionRepository
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.SessionNotFound
import pe.nanamochi.banchus.util.runDatabaseCatching

@Service
class SessionService(private val sessionRepository: SessionRepository) {
    fun findById(id: UUID): Result<Session, SessionNotFound> =
        sessionRepository.findSessionById(id).toResultOr { SessionNotFound }

    fun findAll(): List<Session> = sessionRepository.findAll()

    fun findPrimaryByUsername(username: String): Result<Session, SessionNotFound> =
        sessionRepository.findByUserUsernameIgnoreCaseAndPrimarySessionTrue(username).toResultOr {
            SessionNotFound
        }

    fun findPrimaryByUserId(userId: Int): Result<Session, SessionNotFound> =
        sessionRepository.findByUserIdAndPrimarySessionTrue(userId).toResultOr { SessionNotFound }

    fun create(session: Session): Result<Session, DomainMessage> = runDatabaseCatching {
        if (session.primarySession) {
            session.user?.id?.let { userId ->
                sessionRepository.findByUserIdAndPrimarySessionTrue(userId)?.apply {
                    primarySession = false
                    sessionRepository.save(this)
                }
            }
        }
        sessionRepository.save(session)
    }

    fun update(session: Session): Result<Session, DomainMessage> {
        val id = session.id ?: return Err(SessionNotFound)

        return if (sessionRepository.existsById(id)) {
            runDatabaseCatching { sessionRepository.save(session) }
        } else {
            Err(SessionNotFound)
        }
    }

    fun delete(session: Session): Result<Unit, DomainMessage> {
        val id = session.id ?: return Err(SessionNotFound)

        return if (sessionRepository.existsById(id)) {
            runDatabaseCatching { sessionRepository.delete(session) }
        } else {
            Err(SessionNotFound)
        }
    }
}
