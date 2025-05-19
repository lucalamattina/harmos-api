package ar.edu.itba.harmos.security

import ar.edu.itba.harmos.common.constants.SecurityConstants.AUTHENTICATE_URL
import ar.edu.itba.harmos.common.constants.SecurityConstants.EXPIRATION_TIME
import ar.edu.itba.harmos.common.constants.SecurityConstants.KEY
import ar.edu.itba.harmos.dtos.responses.TokenResponse
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.services.AppUserService
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.io.IOException
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.collections.ArrayList


class AuthenticationFilter(
    private val authenticationManager: AuthenticationManager,
    private val appUserService: AppUserService
) : UsernamePasswordAuthenticationFilter() {

    init {
        this.setFilterProcessesUrl(AUTHENTICATE_URL)
    }

    @Throws(AuthenticationException::class)
    override fun attemptAuthentication(
        req: HttpServletRequest,
        res: HttpServletResponse?
    ): Authentication {
        return try {
            val appUser: AppUser =
                ObjectMapper().readValue(req.inputStream, AppUser::class.java)
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    appUser.email,
                    appUser.password, ArrayList()
                )
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class, ServletException::class)
    override fun successfulAuthentication(
        req: HttpServletRequest?, res: HttpServletResponse, chain: FilterChain?,
        auth: Authentication
    ) {
        val exp = Date(System.currentTimeMillis() + EXPIRATION_TIME)
        val key = Keys.hmacShaKeyFor(KEY.toByteArray())
        val email = (auth.principal as User).username
        val appUser = appUserService.getAppUserByEmail(email) ?: throw RuntimeException("User not found")
        val claims = Jwts.claims().setSubject(appUser.id.toString())
        val token = Jwts.builder()
            .setClaims(claims)
            .signWith(key, SignatureAlgorithm.HS512)
            .setExpiration(exp)
            .compact()
        res.addHeader("token", token)
        val tokenResponse = TokenResponse(token, email)
        res.status = HttpStatus.OK.value()
        val json = ObjectMapper().writeValueAsString(tokenResponse)
        res.writer.write(json)
        res.contentType = "application/json"
        res.flushBuffer()
    }
}