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
import ar.edu.itba.harmos.services.CloudinaryService
import ar.edu.itba.harmos.models.Notification
import ar.edu.itba.harmos.models.EmailTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.transaction.annotation.Transactional

@Service
class AnnouncementService(
    private val announcementRepository: AnnouncementRepository,
    private val specialtyService: SpecialtyService,
    private val asyncNotificationService: AsyncNotificationService,
    private val cloudinaryService: CloudinaryService
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
        val savedAnnouncement = announcementRepository.save(announcement)//TODO: DEBERIA SER EL ID DEL ANUNCIO

        // Process notifications asynchronously using separate service
        asyncNotificationService.processAnnouncementNotificationsAsync(
            savedAnnouncement, 
            specialties.toList(), 
            createdBy
        )

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
        val announcement = getAnnouncementById(id) ?: return false
        
        try {
            println("Deleting announcement $id with ${announcement.images.size} images and ${announcement.files.size} files")
            
            // Eliminar imágenes de Cloudinary
            announcement.images.forEach { imageUrl ->
                try {
                    println("Attempting to delete image: $imageUrl")
                    val deleted = cloudinaryService.deleteFileEnhanced(imageUrl, "image")
                    println("Image deletion result: $deleted")
                    if (!deleted) {
                        println("WARNING: Failed to delete image from Cloudinary: $imageUrl")
                    }
                } catch (e: Exception) {
                    println("Error deleting image from Cloudinary: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            // Eliminar archivos de Cloudinary
            announcement.files.forEach { fileUrl ->
                try {
                    println("Attempting to delete file: $fileUrl")
                    val deleted = cloudinaryService.deleteFileEnhanced(fileUrl, "raw")
                    println("File deletion result: $deleted")
                    if (!deleted) {
                        println("WARNING: Failed to delete file from Cloudinary: $fileUrl")
                    }
                } catch (e: Exception) {
                    println("Error deleting file from Cloudinary: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            // Eliminar anuncio de la base de datos
            println("Deleting announcement from database")
            announcementRepository.deleteById(id)
            println("Announcement $id deleted successfully")
            return true
        } catch (e: Exception) {
            println("Error deleting announcement: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun updateAnnouncementFiles(announcement: Announcement): Announcement {
        return announcementRepository.save(announcement)
    }
}