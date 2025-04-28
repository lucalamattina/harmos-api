package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse.Companion
import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Specialty


data class SpecialtyResponse(
    val id: Long,
    val name: String,
){
    companion object {
        fun singleFromModel(specialty: Specialty) : SpecialtyResponse {
            return SpecialtyResponse(
                specialty.id,
                specialty.name,
            )
        }
        fun listFromModel(specialties: List<Specialty>): List<SpecialtyResponse> {
            return specialties.map { SpecialtyResponse.singleFromModel(it) }
        }
    }
}