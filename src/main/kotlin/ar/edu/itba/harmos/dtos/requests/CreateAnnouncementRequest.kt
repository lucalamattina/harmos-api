package ar.edu.itba.harmos.dtos.requests

import java.time.LocalDateTime

data class CreateAnnouncementRequest(
    val title: String,
    val content: String,
    val date: LocalDateTime,
    val createdByUserId: Long,
    val specialties: List<String> = emptyList() // Opcional, por defecto es una lista vacía
                                        // y se toma como equivalente a todas las specialties
)