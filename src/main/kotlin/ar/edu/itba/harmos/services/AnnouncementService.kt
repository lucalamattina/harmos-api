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
import ar.edu.itba.harmos.services.NotificacionService
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.EmailService
import ar.edu.itba.harmos.models.Notificacion
import ar.edu.itba.harmos.models.EmailTemplate

@Service

class AnnouncementService(
    private val announcementRepository: AnnouncementRepository,
    private val specialtyService: SpecialtyService,
    private val notificacionService: NotificacionService,
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

        // Notificar a todos los usuarios que compartan especialidad
        val specialtyNames = specialties.map { it.name }
        val specialtyList = specialties.toList()
        val usuarios = appUserService.findUsersBySpecialties(specialtyList, 0, 1000)
        usuarios.forEach { usuario ->
            val notificacion = Notificacion(
                mensaje = "Nuevo anuncio: ${announcement.title}",
                usuario = usuario,
                anuncioId = savedAnnouncement.id
            )
            notificacionService.crear(notificacion)

            // Enviar email
            val template = EmailTemplate(
                subject = "Nuevo anuncio: ${announcement.title}",
                body = "<p>${announcement.content}</p>",
                isHtml = true
            )
            emailService.sendEmail(usuario.email, template)
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

}