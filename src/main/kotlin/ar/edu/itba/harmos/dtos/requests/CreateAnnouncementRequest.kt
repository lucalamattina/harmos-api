package ar.edu.itba.harmos.dtos.requests


data class CreateAnnouncementRequest(
    val title: String,
    val content: String,
    val specialties: List<String> = emptyList() // Opcional, por defecto es una lista vacía
                                        // y se toma como equivalente a todas las specialties
)