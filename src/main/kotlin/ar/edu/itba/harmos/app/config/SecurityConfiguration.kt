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


@EnableWebSecurity
@Configuration
class SecurityConfiguration(
    private val appUserDetailsService: AppUserDetailsService,
    private val appUserService: AppUserService,
    private val passwordEncoder: PasswordEncoder
) : WebSecurityConfigurerAdapter() {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource? {
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", CorsConfiguration().applyPermitDefaultValues())
        return source
    }

    @Throws(java.lang.Exception::class)
    override fun configure(http: HttpSecurity) {
        http.cors().and().csrf().disable().authorizeRequests()
            .antMatchers(HttpMethod.POST, AUTHENTICATE_URL).permitAll()
            //.antMatchers(HttpMethod.POST, "/users").hasAuthority(AppUserRole.ADMINISTRATOR.roleName)
            //.anyRequest().authenticated()
            .and()
            .addFilter(AuthenticationFilter(authenticationManager(), appUserService))
            .addFilter(AuthorizationFilter(authenticationManager()))
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    }

    @Throws(Exception::class)
    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(appUserDetailsService).passwordEncoder(passwordEncoder)
    }
}