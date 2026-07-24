package pe.nanamochi.banchus.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import pe.nanamochi.banchus.infrastructure.security.UserArgumentResolver

@Configuration
class WebConfig(private val userArgumentResolver: UserArgumentResolver) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(userArgumentResolver)
    }
}
