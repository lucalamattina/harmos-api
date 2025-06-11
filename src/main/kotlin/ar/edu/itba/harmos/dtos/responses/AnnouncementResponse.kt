package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.models.Specialty
import java.time.LocalDateTime

data class AnnouncementResponse(
    val id: Long,
    val title: String,
    val content: String,
    val date: LocalDateTime,
    val createdByUserId: Long,
    val specialties: List<Specialty>,
    val images: List<String>,
    val files: List<String>)
{
    companion object {
        fun singleFromModel(announcement: Announcement) : AnnouncementResponse {
            return AnnouncementResponse(
                announcement.id,
                announcement.title,
                announcement.content,
                announcement.date,
                announcement.createdBy.id,
                announcement.specialties.toList(),
                announcement.images.toList(),
                announcement.files.toList()
            )
        }
        fun listFromModel(announcements: List<Announcement>): List<AnnouncementResponse> {
            return announcements.map { singleFromModel(it) }
        }
    }
}