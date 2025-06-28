package ar.edu.itba.harmos.dtos.requests

data class EditAppUserRequest(
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val specialties: List<String>? = null,
    val roles: List<String>? = null
) 