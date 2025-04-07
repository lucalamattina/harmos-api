package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.models.Announcement
import java.time.LocalDateTime

data class AnnouncementResponse(
    val id: Long,
    val title: String,
    val content: String,
    val date: LocalDateTime,
    val createdByUserId: Long
) {
    companion object {
        fun singleFromModel(announcement: Announcement) : AnnouncementResponse {
            return AnnouncementResponse(
                announcement.id,
                announcement.title,
                announcement.content,
                announcement.date,
                announcement.createdBy.id,
            )
        }
        fun listFromModel(announcements: List<Announcement>): List<AnnouncementResponse> {
            return announcements.map { singleFromModel(it) }
        }
    }
}