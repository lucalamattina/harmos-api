package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse.Companion
import ar.edu.itba.harmos.models.AppUser

data class AppUserResponse (
    val id: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val specialties: List<String>
) {
    companion object {
        fun singleFromModel(appUser: AppUser) : AppUserResponse {
            return AppUserResponse(
                appUser.id,
                appUser.email,
                appUser.firstName,
                appUser.lastName,
                appUser.phone,
                appUser.specialties.map { it.name }.toList() //TODO: IS THIS OK? fixed?
            )
        }
        fun listFromModel(appUsers: List<AppUser>) : List<AppUserResponse> {
            return appUsers.map { singleFromModel(it) }

        }
    }
}