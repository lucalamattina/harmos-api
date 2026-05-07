package ar.edu.itba.harmos.dtos.requests

data class EditAppUserRequest(
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val specialty: String? = null,
    val roles: List<String>? = null
) 