package ar.edu.itba.harmos.dtos.requests

import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class CreateSpecialtyRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name must not exceed 255 characters")
    val name: String,

    val data: String? = null
)
