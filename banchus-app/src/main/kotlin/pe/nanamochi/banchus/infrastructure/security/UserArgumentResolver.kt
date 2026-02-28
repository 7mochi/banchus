package pe.nanamochi.banchus.infrastructure.security

import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.mapError
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.server.ResponseStatusException
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.service.UserService

@Component
class UserArgumentResolver(private val userService: UserService) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(AuthenticatedUser::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): User {
        val username =
            listOf("u", "us")
                .mapNotNull { webRequest.getParameter(it) }
                .firstOrNull { it.isNotBlank() } ?: ""
        val passwordMd5 =
            listOf("ha", "h", "p")
                .mapNotNull { webRequest.getParameter(it) }
                .firstOrNull { it.length == 32 } ?: ""

        return userService
            .login(username, passwordMd5)
            .mapError { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials") }
            .getOrThrow { it }
    }
}
