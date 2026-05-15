package ar.edu.itba.harmos.dtos.requests

import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class CreateAnnouncementRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 255, message = "Title must not exceed 255 characters")
    val title: String,

    @field:NotBlank(message = "Content is required")
    val content: String,

    val specialties: List<String> = emptyList() // Opcional, por defecto es una lista vacía
                                        // y se toma como equivalente a todas las specialties
)
