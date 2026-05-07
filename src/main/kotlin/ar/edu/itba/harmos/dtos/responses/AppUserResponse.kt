package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse.Companion
import ar.edu.itba.harmos.models.AppUser
import org.springframework.data.domain.Page

data class AppUserResponse (
    val id: Long,
    val email: String,
    val name: String,
    val phone: String,
    val specialty: String?,
    val roles: List<String>
) {
    companion object {
        fun singleFromModel(appUser: AppUser) : AppUserResponse {
            return AppUserResponse(
                appUser.id,
                appUser.email,
                appUser.name,
                appUser.phone,
                appUser.specialty?.name,
                appUser.roles.map { it.role }.toList()
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