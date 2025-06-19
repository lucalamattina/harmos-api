package ar.edu.itba.harmos.app.config

import ar.edu.itba.harmos.common.constants.SecurityConstants.AUTHENTICATE_URL
import ar.edu.itba.harmos.security.AuthenticationFilter
import ar.edu.itba.harmos.security.AuthorizationFilter
import ar.edu.itba.harmos.services.AppUserDetailsService
import ar.edu.itba.harmos.services.AppUserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import javax.servlet.http.HttpServletResponse


@EnableWebSecurity
@Configuration
class SecurityConfiguration(
    private val appUserDetailsService: AppUserDetailsService,
    private val appUserService: AppUserService,
    private val passwordEncoder: PasswordEncoder
) : WebSecurityConfigurerAdapter() {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(
            "https://harmos-web-0d62e723abba.herokuapp.com",
            "http://localhost:3000" // Para desarrollo local
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "X-Requested-With")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Throws(java.lang.Exception::class)
    override fun configure(http: HttpSecurity) {
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
            // Permitir POST en /reports para cualquier usuario autenticado
            .antMatchers(HttpMethod.POST, "/reports").authenticated()
            // Solo los POST requieren rol de administrador
            .antMatchers(HttpMethod.POST, "/users").hasAuthority("ADMINISTRATOR")
            .antMatchers(HttpMethod.POST, "/specialties").hasAuthority("ADMINISTRATOR")
            .anyRequest().authenticated()
            .and()
            .addFilter(AuthenticationFilter(authenticationManager(), appUserService))
            .addFilterAfter(AuthorizationFilter(authenticationManager()), AuthenticationFilter::class.java)
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    }

    @Throws(Exception::class)
    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(appUserDetailsService).passwordEncoder(passwordEncoder)
    }
}