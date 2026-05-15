package ar.edu.itba.harmos.dtos.requests

import javax.validation.constraints.Size

data class EditSpecialtyRequest(
    @field:Size(max = 255, message = "Name must not exceed 255 characters")
    val name: String?,

    val data: String? = null
)
