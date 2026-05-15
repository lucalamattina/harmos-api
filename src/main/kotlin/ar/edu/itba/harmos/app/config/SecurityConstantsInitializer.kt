package ar.edu.itba.harmos.app.config

import ar.edu.itba.harmos.common.constants.SecurityConstants
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class SecurityConstantsInitializer(
    @Value("\${jwt.secret:}") private val jwtSecret: String
) {
    @PostConstruct
    fun init() {
        check(jwtSecret.isNotBlank()) {
            "jwt.secret must be set (via application properties or JWT_SECRET env var). " +
                "HS512 requires at least 64 bytes (512 bits)."
        }
        check(jwtSecret.toByteArray().size >= 64) {
            "jwt.secret must be at least 64 bytes for HS512 (current: ${jwtSecret.toByteArray().size})."
        }
        SecurityConstants.KEY = jwtSecret
    }
}
