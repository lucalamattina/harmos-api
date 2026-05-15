package ar.edu.itba.harmos.dtos.requests

import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class ResetPasswordRequest(
    @field:NotBlank(message = "Token is required")
    val token: String = "",

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    val newPassword: String = ""
)
