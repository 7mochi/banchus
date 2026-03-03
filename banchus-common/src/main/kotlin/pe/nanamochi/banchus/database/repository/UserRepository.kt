package pe.nanamochi.banchus.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.User

@Repository
interface UserRepository : JpaRepository<User, Int> {
    fun findUserById(id: Int): User?

    fun findByUsername(username: String): User?

    fun findByEmail(email: String): User?

    fun findByUsernameAndPasswordMd5(username: String, passwordMd5: String): User?
}
