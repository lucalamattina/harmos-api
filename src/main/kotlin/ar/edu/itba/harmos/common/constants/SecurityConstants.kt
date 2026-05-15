package ar.edu.itba.harmos.common.constants

object SecurityConstants {
    const val AUTHENTICATE_URL = "/authenticate"
    lateinit var KEY: String
    const val HEADER_NAME = "Authorization"
    const val TOKEN_PREFIX = "Bearer "
    const val EXPIRATION_TIME = 1000L * 60 * 30 // 30 minutes
}