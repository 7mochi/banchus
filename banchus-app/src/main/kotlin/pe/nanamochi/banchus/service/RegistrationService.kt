package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.ServerPrivileges
import pe.nanamochi.banchus.domain.errors.CheckOk
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.EmailTaken
import pe.nanamochi.banchus.domain.errors.InvalidEmail
import pe.nanamochi.banchus.domain.errors.InvalidPassword
import pe.nanamochi.banchus.domain.errors.InvalidUsername
import pe.nanamochi.banchus.domain.errors.RegistrationResult
import pe.nanamochi.banchus.domain.errors.UserCreated
import pe.nanamochi.banchus.domain.errors.UsernameTaken
import pe.nanamochi.banchus.util.isValidEmail
import pe.nanamochi.banchus.util.isValidPassword
import pe.nanamochi.banchus.util.isValidUsername
import pe.nanamochi.banchus.util.toMd5

@Service
class RegistrationService(
    private val userService: UserService,
    private val statService: StatService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun registerUser(
        username: String,
        email: String,
        passwordPlainText: String,
        check: Int,
    ): Result<RegistrationResult, List<DomainMessage>> {
        if (check != 0) return Ok(CheckOk)

        val errors = buildList {
            if (!username.isValidUsername()) add(InvalidUsername)
            if (!email.isValidEmail()) add(InvalidEmail)
            if (!passwordPlainText.isValidPassword()) add(InvalidPassword)

            if (userService.findByUsername(username).isOk) add(UsernameTaken)
            if (userService.findByEmail(email).isOk) add(EmailTaken)
        }

        if (errors.isNotEmpty()) return Err(errors)

        return binding {
                val createdUser =
                    userService
                        .create(
                            User(
                                username = username,
                                email = email,
                                passwordMd5 = passwordPlainText.toMd5(),
                                country = CountryCode.KP,
                                privileges = ServerPrivileges.UNRESTRICTED.value,
                            )
                        )
                        .bind()

                statService.createAllGamemodes(createdUser).bind()

                log.debug(
                    "User registered with username: {} and email: {}",
                    createdUser.username,
                    createdUser.email,
                )

                UserCreated(createdUser)
            }
            .mapError { listOf(it) }
    }
}
