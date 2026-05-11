package ar.edu.itba.harmos.common.constants

object SecurityConstants {
    const val AUTHENTICATE_URL = "/authenticate"
    val KEY: String = System.getenv("JWT_SECRET") ?: error("JWT_SECRET env var not set")
    const val HEADER_NAME = "Authorization"
    const val TOKEN_PREFIX = "Bearer "
    const val EXPIRATION_TIME = 1000L * 60 * 30 // 30 minutes
}