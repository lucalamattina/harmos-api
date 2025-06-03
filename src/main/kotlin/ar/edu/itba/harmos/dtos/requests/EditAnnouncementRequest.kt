package ar.edu.itba.harmos.dtos.requests

data class EditAnnouncementRequest(
    val title: String?,
    val content: String?,
    val specialties: List<String>? = null
) 