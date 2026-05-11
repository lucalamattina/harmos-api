package ar.edu.itba.harmos.dtos.requests

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class EmailRequest(
    @field:NotBlank(message = "Recipient email is required")
    @field:Email(message = "Recipient must be a valid email address")
    @field:Size(max = 255, message = "Recipient email must not exceed 255 characters")
    val to: String,

    @field:NotBlank(message = "Subject is required")
    @field:Size(max = 255, message = "Subject must not exceed 255 characters")
    val subject: String,

    @field:NotBlank(message = "Body is required")
    val body: String
)
