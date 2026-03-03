package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.database.repository.UserRepository
import pe.nanamochi.banchus.domain.errors.DatabaseError
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.InvalidCredentials
import pe.nanamochi.banchus.domain.errors.UserNotFound
import pe.nanamochi.banchus.util.runDatabaseCatching

@Service
class UserService(private val userRepository: UserRepository) {

    fun findById(id: Int): Result<User, UserNotFound> =
        userRepository.findUserById(id).toResultOr { UserNotFound }

    fun findByUsername(username: String): Result<User, UserNotFound> =
        userRepository.findByUsername(username).toResultOr { UserNotFound }

    fun findByEmail(email: String): Result<User, UserNotFound> =
        userRepository.findByEmail(email).toResultOr { UserNotFound }

    fun login(username: String, passwordMd5: String): Result<User, InvalidCredentials> =
        userRepository.findByUsernameAndPasswordMd5(username, passwordMd5).toResultOr {
            InvalidCredentials
        }

    fun create(user: User): Result<User, DatabaseError> = runDatabaseCatching {
        userRepository.save(user)
    }

    fun update(user: User): Result<User, DomainMessage> =
        if (userRepository.existsById(user.id)) {
            runDatabaseCatching { userRepository.save(user) }
        } else {
            Err(UserNotFound)
        }
}
