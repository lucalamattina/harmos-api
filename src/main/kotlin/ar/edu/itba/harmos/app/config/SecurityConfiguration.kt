package ar.edu.itba.harmos.app.config

import ar.edu.itba.harmos.common.constants.SecurityConstants.AUTHENTICATE_URL
import ar.edu.itba.harmos.security.AuthenticationFilter
import ar.edu.itba.harmos.security.AuthorizationFilter
import ar.edu.itba.harmos.services.AppUserDetailsService
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.models.AppUserRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import javax.servlet.http.HttpServletResponse


@EnableWebSecurity
@Configuration
class SecurityConfiguration(
    private val appUserDetailsService: AppUserDetailsService,
    private val appUserService: AppUserService,
    private val passwordEncoder: PasswordEncoder,
    @org.springframework.beans.factory.annotation.Value("\${app.frontend.url}") private val frontendUrl: String
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(
            frontendUrl,
            "http://localhost:3000"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "X-Requested-With")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Autowired
    fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(appUserDetailsService).passwordEncoder(passwordEncoder)
    }

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager {
        return authenticationConfiguration.authenticationManager
    }

    @Bean
    fun filterChain(http: HttpSecurity, authenticationManager: AuthenticationManager): SecurityFilterChain {
        val authManager = authenticationManager

        http.cors().and().csrf().disable()
            .exceptionHandling()
            .authenticationEntryPoint { request, response, authException ->
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.writer.write("Unauthorized: ${authException.message}")
            }
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Permitir OPTIONS para CORS
            .antMatchers(AUTHENTICATE_URL).permitAll() // Permitir cualquier método en /authenticate
            // Permitir endpoints de recuperación de contraseña sin autenticación
            .antMatchers(HttpMethod.POST, "/users/forgot-password").permitAll()
            .antMatchers(HttpMethod.GET, "/users/validate-reset-token").permitAll()
            .antMatchers(HttpMethod.POST, "/users/reset-password").permitAll()
            // Permitir POST en /reports para cualquier usuario autenticado
            .antMatchers(HttpMethod.POST, "/reports").authenticated()
            // Permitir a todos los usuarios autenticados ver todos los reportes
            .antMatchers(HttpMethod.GET, "/reports/all").authenticated()
            // Solo los POST y DELETE requieren rol de administrador
            .antMatchers(HttpMethod.POST, "/users").hasAuthority(AppUserRole.ADMINISTRATOR.roleName)
            .antMatchers(HttpMethod.DELETE, "/users/**").hasAuthority(AppUserRole.ADMINISTRATOR.roleName)
            .antMatchers(HttpMethod.PUT, "/users/**").hasAuthority(AppUserRole.ADMINISTRATOR.roleName)
            .antMatchers(HttpMethod.POST, "/specialties").hasAuthority(AppUserRole.ADMINISTRATOR.roleName)
            .antMatchers(HttpMethod.POST, "/schedules").hasAuthority(AppUserRole.ADMINISTRATOR.roleName)
            .antMatchers(HttpMethod.DELETE, "/schedules/**").hasAuthority(AppUserRole.ADMINISTRATOR.roleName)
            // Restringir la asignación de doctores a pacientes solo a administradores
            .antMatchers(HttpMethod.POST, "/patients/*/doctors/*").hasAuthority(AppUserRole.ADMINISTRATOR.roleName)
            .anyRequest().authenticated()
            .and()
            .addFilter(AuthenticationFilter(authManager, appUserService))
            .addFilterAfter(AuthorizationFilter(authManager), AuthenticationFilter::class.java)
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

        return http.build()
    }
}
