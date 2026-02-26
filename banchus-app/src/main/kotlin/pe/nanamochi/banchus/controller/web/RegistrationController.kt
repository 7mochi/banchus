package pe.nanamochi.banchus.controller.web

import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onSuccess
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pe.nanamochi.banchus.domain.errors.CheckOk
import pe.nanamochi.banchus.domain.errors.EmailTaken
import pe.nanamochi.banchus.domain.errors.InvalidFormat
import pe.nanamochi.banchus.domain.errors.UserCreated
import pe.nanamochi.banchus.domain.errors.UsernameTaken
import pe.nanamochi.banchus.service.RegistrationService

@RestController
@RequestMapping("/")
class RegistrationController(private val registrationService: RegistrationService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/users")
    fun registerAccount(
        @RequestParam("user[username]") username: String,
        @RequestParam("user[user_email]") email: String,
        @RequestParam("user[password]") password: String,
        @RequestParam("check") check: Int,
    ): ResponseEntity<*> {
        return registrationService
            .registerUser(username, email, password, check)
            .onSuccess { result ->
                when (result) {
                    is UserCreated ->
                        log.debug(
                            "User registered with username: {} and email: {}",
                            result.user.username,
                            result.user.email,
                        )
                    is CheckOk ->
                        log.debug(
                            "Check-only request for registration with username: {} and email: {}",
                            username,
                            email,
                        )
                }
            }
            .map { ResponseEntity.ok("ok") }
            .getOrElse { domainErrors ->
                val errorsMap = mutableMapOf<String, MutableList<String>>()

                domainErrors.forEach { error ->
                    when (error) {
                        is UsernameTaken ->
                            errorsMap.addError("username", "Username already taken.")
                        is EmailTaken -> errorsMap.addError("user_email", "Email already taken.")
                        is InvalidFormat -> errorsMap.addError(error.field, error.message)
                        else -> errorsMap.addError("password", "An internal error occurred.")
                    }
                }

                ResponseEntity.badRequest().body(mapOf("form_error" to mapOf("user" to errorsMap)))
            }
    }

    private fun MutableMap<String, MutableList<String>>.addError(field: String, message: String) {
        this.getOrPut(field) { mutableListOf() }.add(message)
    }
}
