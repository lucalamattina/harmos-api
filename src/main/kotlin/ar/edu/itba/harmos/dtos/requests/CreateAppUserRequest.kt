package ar.edu.itba.harmos.dtos.requests

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class CreateAppUserRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be a valid email address")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,

    @field:NotBlank(message = "First name is required")
    @field:Size(max = 255, message = "First name must not exceed 255 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 255, message = "Last name must not exceed 255 characters")
    val lastName: String,

    @field:NotBlank(message = "Phone is required")
    @field:Size(max = 50, message = "Phone must not exceed 50 characters")
    val phone: String,

    val specialty: String? = null,
    val roles: List<String>? = emptyList()
)
