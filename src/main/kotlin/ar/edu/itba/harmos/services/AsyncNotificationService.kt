package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Notification
import ar.edu.itba.harmos.models.EmailTemplate
import ar.edu.itba.harmos.models.Report
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
            val announcementLink = "$frontendUrl/announcements/${announcement.id}"
            val template = EmailTemplate.announcementNotification(
                announcementTitle = announcement.title,
                announcementContent = announcement.content,
                link = announcementLink,
                author = "${announcement.createdBy.firstName} ${announcement.createdBy.lastName}",
                date = announcement.date.toString(),
                specialties = announcement.specialties.joinToString(", ") { it.name }
            )

            emailService.sendBatch(users.map { it.email }, template)
            logger.info("Completed async email sending for announcement ${announcement.id} (${users.size} recipients)")

        } catch (e: Exception) {
            logger.error("Error sending announcement emails: ${e.message}", e)
        }
    }

    @Async("taskExecutor")
    fun sendReportCreatedEmailAsync(report: Report, recipients: List<AppUser>, creator: AppUser) {
        logger.debug("Starting async email sending for created report ${report.id} on thread: ${Thread.currentThread().name}")
        try {
            if (recipients.isEmpty()) return

            val reportLink = "$frontendUrl/reports/${report.id}"
            val template = EmailTemplate.reportCreated(
                reportTitle = report.title,
                patientName = "${report.patient.firstName} ${report.patient.lastName}",
                creatorName = "${creator.firstName} ${creator.lastName}",
                link = reportLink
            )
            emailService.sendBatch(recipients.map { it.email }, template)
            logger.info("Sent report created email to ${recipients.size} recipient(s) for report ${report.id}")
        } catch (e: Exception) {
            logger.error("Error sending report created email for report ${report.id}: ${e.message}", e)
        }
    }

    @Async("taskExecutor")
    fun sendReportModifiedEmailAsync(report: Report, editor: AppUser) {
        logger.debug("Starting async email sending for modified report ${report.id} on thread: ${Thread.currentThread().name}")
        try {
            val reportLink = "$frontendUrl/reports/${report.id}"
            val owner = report.doctor

            if (owner.id != editor.id) {
                val template = EmailTemplate.reportModified(
                    reportTitle = report.title,
                    patientName = "${report.patient.firstName} ${report.patient.lastName}",
                    editorName = "${editor.firstName} ${editor.lastName}",
                    link = reportLink
                )
                emailService.sendEmail(owner.email, template)
                logger.info("Sent report modified email to ${owner.email} for report ${report.id}")
            }
        } catch (e: Exception) {
            logger.error("Error sending report modified email for report ${report.id}: ${e.message}", e)
        }
    }
}