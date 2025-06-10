package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateAnnouncementRequest
import ar.edu.itba.harmos.dtos.requests.EditAnnouncementRequest
import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.persistence.AnnouncementRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.data.domain.Page
import java.time.LocalDateTime
import ar.edu.itba.harmos.services.NotificationService
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.EmailService
import ar.edu.itba.harmos.models.Notification
import ar.edu.itba.harmos.models.EmailTemplate

@Service
class AnnouncementService(
    private val announcementRepository: AnnouncementRepository,
    private val specialtyService: SpecialtyService,
    private val notificationService: NotificationService,
    private val appUserService: AppUserService,
    private val emailService: EmailService
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
            LocalDateTime.now(),
            specialties,
            createdBy
        )
        val savedAnnouncement = announcementRepository.save(announcement)

        // Notify all users that share the specialty
        val specialtyNames = specialties.map { it.name }
        val specialtyList = specialties.toList()
        val users = appUserService.findUsersBySpecialties(specialtyList, 0, 1000)
        users.forEach { user ->
            val notification = Notification(
                message = "New announcement: ${announcement.title}",
                user = user,
                announcementId = savedAnnouncement.id
            )
            notificationService.create(notification)

            // Send email
            val link = "https://tusitio.com/announcements/${savedAnnouncement.id}" // Change to your actual frontend URL
            val template = EmailTemplate.announcementNotification(
                announcement.title,
                announcement.content,
                link
            )
            emailService.sendEmail(user.email, template)
        }

        return savedAnnouncement
    }

    fun searchAnnouncementsByPage(page: Int, offset: Int, specialties: List<String>?): Page<Announcement> {
        val pageable = PageRequest.of(page, offset, Sort.by(Sort.Direction.DESC, "date"))
        return if (specialties == null) {
            announcementRepository.findAll(pageable)
        } else {
            announcementRepository.findBySpecialtiesIn(specialties, pageable)
        }
    }

    fun searchAnnouncementsByCreatedByAndPage(createdBy: AppUser, page: Int, offset: Int): List<Announcement> {
        val pageable = PageRequest.of(page, offset, Sort.by(Sort.Direction.DESC, "date"))
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

    fun updateAnnouncement(id: Long, editRequest: EditAnnouncementRequest): Announcement {
        val existingAnnouncement = announcementRepository.findById(id)
            .orElseThrow { RuntimeException("Announcement not found with id $id") }

        editRequest.title?.let { existingAnnouncement.title = it }
        editRequest.content?.let { existingAnnouncement.content = it }
        editRequest.specialties?.let { names ->
            val specialties = names.mapNotNull { specialtyService.getSpecialtyByName(it) }.toMutableSet()
            if (specialties.isNotEmpty()) {
                existingAnnouncement.specialties.clear()
                existingAnnouncement.specialties.addAll(specialties)
            }
        }
        return announcementRepository.save(existingAnnouncement)
    }

    fun deleteAnnouncement(id: Long): Boolean {
        val exists = announcementRepository.existsById(id)
        if (!exists) return false
        announcementRepository.deleteById(id)
        return true
    }

    fun updateAnnouncementFiles(announcement: Announcement): Announcement {
        return announcementRepository.save(announcement)
    }
}