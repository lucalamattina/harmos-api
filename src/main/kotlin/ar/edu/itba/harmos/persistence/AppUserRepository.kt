package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.AppUser
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Page
import org.springframework.data.repository.PagingAndSortingRepository


@Repository
interface AppUserRepository : PagingAndSortingRepository<AppUser, Long> {
    fun findByEmail(email: String): AppUser?

    fun findBySpecialtiesIn(specialties: List<String>, pageable: Pageable): List<AppUser>

    fun findBySpecialtiesInAndIdOrEmail(specialties: List<String>, id: Long?, email: String?): List<AppUser>


}