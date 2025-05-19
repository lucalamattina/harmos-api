package ar.edu.itba.harmos.security.resolvers

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.security.annotations.CurrentUser
import ar.edu.itba.harmos.services.AppUserService
import io.jsonwebtoken.Claims
import org.springframework.context.annotation.Lazy
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserArgumentResolver(
    @Lazy private val appUserService: AppUserService
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.getParameterAnnotation(CurrentUser::class.java) != null &&
                parameter.parameterType == AppUser::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): AppUser? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null

        val principal = authentication.principal
        if (principal !is Claims) {
            return null
        }

        val userId = principal.subject
        if (userId.isNullOrBlank()) {
            return null
        }

        return appUserService.getAppUserById(userId.toLong())
    }
}
    