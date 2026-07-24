package pe.nanamochi.banchus.identity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.identity.entity.User

@Repository
interface UserRepository : JpaRepository<User, Int> {
    fun findByUsername(username: String): User?

    fun findByEmail(email: String): User?

    fun findUserById(id: Int): User?
}
