package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Specialty
import org.springframework.data.domain.Page
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

@Repository
interface AnnouncementRepository : PagingAndSortingRepository<Announcement, Long> {
    fun findByCreatedBy(appUser: AppUser, pageable: Pageable): Page<Announcement>

    @Query("""
        SELECT a FROM Announcement a 
        WHERE EXISTS (
            SELECT 1 FROM a.specialties s 
            WHERE s.name IN :specialties
        )
    """)
    fun findBySpecialtiesIn(@Param("specialties") specialties: List<String>, pageable: Pageable): Page<Announcement>
}