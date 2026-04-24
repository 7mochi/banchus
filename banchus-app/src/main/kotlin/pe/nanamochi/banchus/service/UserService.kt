package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.toResultOr
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.database.repository.UserRepository
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.domain.error.InvalidCredentials
import pe.nanamochi.banchus.domain.error.UserNotFound
import pe.nanamochi.banchus.util.runDatabaseCatching

@Service
class UserService(private val userRepository: UserRepository) {
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
}
