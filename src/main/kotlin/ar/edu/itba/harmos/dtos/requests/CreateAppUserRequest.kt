package ar.edu.itba.harmos.dtos.requests

data class CreateAppUserRequest (
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val specialties: List<String>? = null,
    val roles: List<String>? = emptyList()
)