package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Notification
import ar.edu.itba.harmos.models.EmailTemplate
import ar.edu.itba.harmos.models.Specialty
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory

@Service
class AsyncNotificationService(
    private val notificationService: NotificationService,
    private val appUserService: AppUserService,
    private val emailService: EmailService,
    @Value("\${app.frontend.url}") private val frontendUrl: String
) {
    private val logger = LoggerFactory.getLogger(AsyncNotificationService::class.java)

    @Async("taskExecutor")
    @Transactional
    fun processAnnouncementNotificationsAsync(announcement: Announcement, specialties: List<Specialty>, createdBy: AppUser) {
        logger.info("Starting async notification processing for announcement ${announcement.id} on thread: ${Thread.currentThread().name}")
        
        try {
            // Get all users with matching specialties in batches
            val batchSize = 100
            var page = 0
            var hasMoreUsers = true
            var totalNotifications = 0
            
            while (hasMoreUsers) {
                val users = appUserService.findUsersBySpecialties(specialties, page, batchSize)
                hasMoreUsers = users.size == batchSize
                
                if (users.isEmpty()) break
                
                // Create notifications in batch
                val notifications = users
                    .filter { it.id != createdBy.id }
                    .map { user ->
                        Notification(
                            message = "New announcement: ${announcement.title}",
                            user = user,
                            announcementId = announcement.id
                        )
                    }
                
                // Batch save notifications
                if (notifications.isNotEmpty()) {
                    notificationService.createBatch(notifications)
                    totalNotifications += notifications.size
                    logger.debug("Created ${notifications.size} notifications for page $page")
                    
                    // Send emails asynchronously in background
                    sendAnnouncementEmailsAsync(announcement, users.filter { it.id != createdBy.id })
                }
                
                page++
            }
            
            logger.info("Completed async notification processing for announcement ${announcement.id}. Total notifications: $totalNotifications")
            
        } catch (e: Exception) {
            logger.error("Error processing notifications for announcement ${announcement.id}: ${e.message}", e)
        }
    }

    @Async("taskExecutor")
    fun sendAnnouncementEmailsAsync(announcement: Announcement, users: List<AppUser>) {
        logger.debug("Starting async email sending for ${users.size} users on thread: ${Thread.currentThread().name}")
        
        try {
            // Usar la URL del frontend desde properties
            val announcementLink = "$frontendUrl/announcements/${announcement.id}"
            
            // Send emails in batches to avoid overwhelming the email service
            users.chunked(10).forEachIndexed { batchIndex, userBatch ->
                userBatch.forEach { user ->
                    try {
                        // Usar el template existente con mejor información
                        val template = EmailTemplate.announcementNotification(
                            announcementTitle = announcement.title,
                            announcementContent = announcement.content,
                            link = announcementLink,
                            author = announcement.createdBy.name,
                            date = announcement.date.toString(),
                            specialties = announcement.specialties.joinToString(", ") { it.name }
                        )
                        
                        // Usar el EmailService existente
                        emailService.sendEmail(user.email, template)
                        
                    } catch (e: Exception) {
                        logger.warn("Failed to send email to ${user.email}: ${e.message}")
                    }
                }
                logger.debug("Sent email batch ${batchIndex + 1} (${userBatch.size} emails)")
                
                // Small delay between batches to not overwhelm email service
                Thread.sleep(100)
            }
            
            logger.info("Completed async email sending for announcement ${announcement.id}")
            
        } catch (e: Exception) {
            logger.error("Error sending announcement emails: ${e.message}", e)
        }
    }
} 