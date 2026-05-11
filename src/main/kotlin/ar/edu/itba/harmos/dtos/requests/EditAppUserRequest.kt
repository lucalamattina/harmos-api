package ar.edu.itba.harmos.dtos.requests

import javax.validation.constraints.Size

data class EditAppUserRequest(
    @field:Size(max = 255, message = "First name must not exceed 255 characters")
    val firstName: String?,

    @field:Size(max = 255, message = "Last name must not exceed 255 characters")
    val lastName: String?,

    @field:Size(max = 50, message = "Phone must not exceed 50 characters")
    val phone: String?,

    val specialty: String? = null,
    val roles: List<String>? = null
)
