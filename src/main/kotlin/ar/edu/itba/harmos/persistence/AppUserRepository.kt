package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.AppUser
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository


@Repository
interface AppUserRepository : PagingAndSortingRepository<AppUser, Long> {

    @Query(
        """
        SELECT u FROM AppUser u
        WHERE (:email IS NULL OR u.email LIKE %:email%)
        AND (:specialties IS NULL OR EXISTS (
            SELECT s FROM u.specialties s WHERE s.name IN :specialties
        ))
        """
    )
    fun findAppUsersByEmailAndSpecialties(
        email: String? = null,
        specialties: List<String>? = null,
        pageable: Pageable? = null
    ): Page<AppUser>


    fun findByEmail(email: String): AppUser?

    fun findBySpecialtiesIn(specialties: List<String>, pageable: Pageable): List<AppUser>

    fun findBySpecialtiesInAndIdOrEmail(specialties: List<String>, id: Long?, email: String?): List<AppUser>


}