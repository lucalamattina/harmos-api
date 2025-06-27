package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Specialty
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AppUserRepository : PagingAndSortingRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

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

    @Query("""
        SELECT DISTINCT u FROM AppUser u 
        WHERE EXISTS (
            SELECT 1 FROM u.specialties s 
            WHERE s IN :specialties
        )
    """)
    fun findBySpecialtiesIn(@Param("specialties") specialties: List<Specialty>, pageable: Pageable): List<AppUser>

    fun findBySpecialtiesInAndIdOrEmail(specialties: List<String>, id: Long?, email: String?): List<AppUser>

}