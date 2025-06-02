package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Specialty
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param

@Repository
interface AppUserRepository : PagingAndSortingRepository<AppUser, Long> {

    @Query("""
        SELECT u FROM AppUser u 
        WHERE (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
        AND (:name IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) 
            OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:specialties IS NULL OR EXISTS (
            SELECT 1 FROM u.specialties s 
            WHERE s IN :specialties
        ))
    """)
    fun findAppUsersByEmailAndSpecialties(
        @Param("email") email: String?,
        @Param("name") name: String?,
        @Param("specialties") specialties: List<Specialty>?,
        pageable: Pageable
    ): Page<AppUser>

    @Query("""
        SELECT u FROM AppUser u WHERE LOWER(u.email) = LOWER(:email)
    """)
    fun findByEmail(@Param("email") email: String): AppUser?

    fun findBySpecialtiesIn(specialties: List<String>, pageable: Pageable): List<AppUser>

    fun findBySpecialtiesInAndIdOrEmail(specialties: List<String>, id: Long?, email: String?): List<AppUser>

}