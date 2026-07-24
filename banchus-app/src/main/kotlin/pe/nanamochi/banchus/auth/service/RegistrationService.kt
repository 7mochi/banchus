package pe.nanamochi.banchus.auth.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.identity.entity.User
import pe.nanamochi.banchus.core.enums.ServerPrivileges
import pe.nanamochi.banchus.identity.service.GeolocationService
import pe.nanamochi.banchus.identity.service.UserService
import pe.nanamochi.banchus.identity.util.isValidEmail
import pe.nanamochi.banchus.identity.util.isValidPassword
import pe.nanamochi.banchus.identity.util.isValidUsername
import pe.nanamochi.banchus.score.service.StatService
import pe.nanamochi.banchus.core.enums.CountryCode
import pe.nanamochi.banchus.core.error.CheckOk
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.EmailTaken
import pe.nanamochi.banchus.core.error.InvalidEmail
import pe.nanamochi.banchus.core.error.InvalidPassword
import pe.nanamochi.banchus.core.error.InvalidUsername
import pe.nanamochi.banchus.core.error.RegistrationResult
import pe.nanamochi.banchus.core.error.UserCreated
import pe.nanamochi.banchus.core.error.UsernameTaken
import pe.nanamochi.banchus.core.util.toMd5

@Service
class RegistrationService(
    private val userService: UserService,
    private val statService: StatService,
    private val geolocationService: GeolocationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun registerUser(
        username: String,
        passwordPlainText: String,
        email: String,
        check: Int,
        headers: HttpHeaders,
    ): Result<RegistrationResult, List<DomainMessage>> {
        if (check != 0) return Ok(CheckOk)

        val errors = buildList {
            if (!username.isValidUsername()) add(InvalidUsername)
            if (!email.isValidEmail()) add(InvalidEmail)
            if (!passwordPlainText.isValidPassword()) add(InvalidPassword)

            if (userService.fetchOneByUsername(username).isOk) add(UsernameTaken)
            if (userService.fetchOneByEmail(email).isOk) add(EmailTaken)
        }

        if (errors.isNotEmpty()) return Err(errors)

        val (_, geolocation) = geolocationService.resolve(headers)

        return binding {
                val createdUser =
                    userService
                        .create(
                            User(
                                username = username,
                                safeUsername = username.replace(" ", "_"),
                                email = email,
                                passwordBcrypt =
                                    BCrypt.hashpw(passwordPlainText.toMd5(), BCrypt.gensalt()),
                                country = CountryCode.fromCode(geolocation.countryCode),
                                privileges = ServerPrivileges.UNRESTRICTED.value,
                            )
                        )
                        .bind()

                statService.createAllModes(createdUser).bind()

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
