package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse.Companion
import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Specialty


data class SpecialtyResponse(
    val id: Long,
    val name: String,
    val users: List<Long>? = null,
    val announcements: List<Long>? = null
){
    companion object {
        fun singleFromModel(specialty: Specialty) : SpecialtyResponse {
            return SpecialtyResponse(
                specialty.id,
                specialty.name,
                specialty.users.map{ it.id },
                specialty.announcements.map{ it.id },
            )
        }
        fun listFromModel(specialties: List<Specialty>): List<SpecialtyResponse> {
            return specialties.map { SpecialtyResponse.singleFromModel(it) }
        }
    }
}