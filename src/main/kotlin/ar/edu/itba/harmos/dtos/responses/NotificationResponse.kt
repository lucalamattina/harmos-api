package ar.edu.itba.harmos.dtos.responses

import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val message: String,
    val read: Boolean,
    val date: LocalDateTime,
    val announcementId: Long?
) 