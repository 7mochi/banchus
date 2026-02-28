package pe.nanamochi.banchus.database.repository

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.Session

@Repository
interface SessionRepository : JpaRepository<Session, UUID> {
    fun findSessionById(id: UUID): Session?

    fun findByUserIdAndPrimarySessionTrue(userId: Int): Session?

    fun findByUserUsernameIgnoreCaseAndPrimarySessionTrue(username: String): Session?
}
