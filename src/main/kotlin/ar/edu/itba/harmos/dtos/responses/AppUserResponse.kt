package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse.Companion
import ar.edu.itba.harmos.models.AppUser
import org.springframework.data.domain.Page

data class AppUserResponse (
    val id: Long,
    val email: String,
    val name: String,
    val phone: String,
    val specialties: List<String>
) {
    companion object {
        fun singleFromModel(appUser: AppUser) : AppUserResponse {
            return AppUserResponse(
                appUser.id,
                appUser.email,
                appUser.name,
                appUser.phone,
                appUser.specialties.map { it.name }.toList()
            )
        }
        fun listFromModel(appUsers: List<AppUser>) : List<AppUserResponse> {
            return appUsers.map { singleFromModel(it) }
        }
        fun pageFromModel(appUsers: Page<AppUser>) : Page<AppUserResponse> {
            return appUsers.map { singleFromModel(it) }
        }
    }
}