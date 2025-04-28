package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Specialty
import org.springframework.data.domain.Page
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Pageable

@Repository
interface AnnouncementRepository : PagingAndSortingRepository<Announcement, Long> {
    fun findByCreatedBy(appUser: AppUser, pageable: Pageable): Page<Announcement>

    fun findBySpecialtiesIn(specialties: List<String>, pageable: Pageable): Page<Announcement>

}