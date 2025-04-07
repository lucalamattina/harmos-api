package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateAnnouncementRequest
import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.persistence.AnnouncementRepository
import ar.edu.itba.harmos.persistence.SpecialtyRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class AnnouncementService(
    private val announcementRepository: AnnouncementRepository,
    private val specialtyService: SpecialtyService
) {
    fun createAnnouncement(createAnnouncementRequest: CreateAnnouncementRequest, createdBy: AppUser): Announcement? {

        var specialties = createAnnouncementRequest.specialties.mapNotNull { specialtyName ->
            specialtyService.getSpecialtyByName(specialtyName)
        }.toMutableSet()

        if (specialties.isEmpty()) {
            specialties = specialtyService.getAllSpecialties().toMutableSet()
        }

        val announcement = Announcement(
            createAnnouncementRequest.title,
            createAnnouncementRequest.content,
            createAnnouncementRequest.date,
            specialties,
            createdBy
        )
        return announcementRepository.save(announcement)
    }

    fun searchAnnouncementsByPage(page: Int, offset: Int, specialties: List<String>?): List<Announcement> {
        val pageable = PageRequest.of(page, offset)
        val announcementsPage = if (specialties == null) {
            announcementRepository.findAll(pageable).content
        } else {
                announcementRepository.findBySpecialtiesIn(specialties, pageable)
        }
        return announcementsPage
    }

    fun searchAnnouncementsByCreatedByAndPage(createdBy: AppUser, page: Int, offset: Int): List<Announcement> {
        val pageable = PageRequest.of(page, offset)
        val announcementsPage = announcementRepository.findByCreatedBy(createdBy, pageable)
        return announcementsPage.content
    }

    fun getAnnouncementById(id: Long): Announcement? {
        val opt = announcementRepository.findById(id)
        if (opt.isPresent) {
            return opt.get()
        }
        return null
    }

    fun updateAnnouncement(id: Long, updatedAnnouncement: Announcement): Announcement {
        val existingAnnouncement = announcementRepository.findById(id)
            .orElseThrow { RuntimeException("Announcement not found with id $id") }

        existingAnnouncement.title = updatedAnnouncement.title
        existingAnnouncement.content = updatedAnnouncement.content

        return announcementRepository.save(existingAnnouncement)
    }

}