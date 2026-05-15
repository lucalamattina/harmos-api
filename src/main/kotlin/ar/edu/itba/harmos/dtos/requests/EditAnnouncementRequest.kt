package ar.edu.itba.harmos.dtos.requests

import javax.validation.constraints.Size

data class EditAnnouncementRequest(
    @field:Size(max = 255, message = "Title must not exceed 255 characters")
    val title: String?,

    val content: String?,

    val specialties: List<String>? = null
)
