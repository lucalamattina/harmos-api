package ar.edu.itba.harmos.security

import ar.edu.itba.harmos.common.constants.SecurityConstants.HEADER_NAME
import ar.edu.itba.harmos.common.constants.SecurityConstants.KEY
import ar.edu.itba.harmos.common.constants.SecurityConstants.TOKEN_PREFIX
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class AuthorizationFilter(authManager: AuthenticationManager) : BasicAuthenticationFilter(authManager) {
    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val header: String? = request.getHeader(HEADER_NAME)
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            chain.doFilter(request, response)
            return
        }
        val authentication = authenticate(request)
        SecurityContextHolder.getContext().authentication = authentication
        chain.doFilter(request, response)
    }

    private fun authenticate(request: HttpServletRequest): UsernamePasswordAuthenticationToken? {
        val token: String? = request.getHeader(HEADER_NAME)
        if (token != null) {
            val user: Claims? = Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(KEY.toByteArray())).build()
                .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                .body
            return if (user != null) {
                UsernamePasswordAuthenticationToken(user, null, ArrayList())
            } else {
                null
            }
        }
        return null
    }
}