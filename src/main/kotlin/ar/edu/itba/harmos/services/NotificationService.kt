package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.models.Notification
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.persistence.NotificationRepository
import ar.edu.itba.harmos.exceptions.UnauthorizedNotificationAccessException
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import javax.transaction.Transactional
import java.util.NoSuchElementException
import java.lang.RuntimeException

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    fun getUnread(user: AppUser): List<Notification> {
        logger.debug("Finding unread notifications for user ${user.id}")
        return notificationRepository.findByUserAndReadFalseOrderByDateDesc(user)
    }

    fun getAll(user: AppUser): List<Notification> {
        logger.debug("Finding all notifications for user ${user.id}")
        return notificationRepository.findByUserOrderByDateDesc(user)
    }

    fun getById(id: Long, currentUser: AppUser): Notification {
        logger.debug("Getting notification $id for user ${currentUser.id}")
        val notification = notificationRepository.findById(id).orElseThrow {
            NoSuchElementException("Notification with ID $id not found")
        }
        
        if (notification.user.id != currentUser.id) {
            logger.warn("User ${currentUser.id} attempted to access notification ${notification.id} which belongs to user ${notification.user.id}")
            throw UnauthorizedNotificationAccessException(notification.id, currentUser.id)
        }
        
        return notification
    }

    @Transactional
    fun markAsRead(id: Long, user: AppUser): Notification {
        logger.debug("Attempting to mark notification $id as read for user ${user.id}")
        
        val notification = getById(id, user)

        if (notification.read) {
            logger.debug("Notification $id was already marked as read")
            return notification
        }
        
        return try {
            logger.debug("Updating notification $id as read")
            val updated = Notification(
                message = notification.message,
                read = true,
                date = notification.date,
                user = notification.user,
                id = notification.id,
                announcementId = notification.announcementId
            )
            notificationRepository.save(updated)
        } catch (e: Exception) {
            logger.error("Error saving notification $id as read", e)
            throw RuntimeException("Error marking notification as read", e)
        }
    }

    fun create(notification: Notification): Notification {
        logger.debug("Creating new notification for user ${notification.user.id}")
        return try {
            notificationRepository.save(notification)
        } catch (e: Exception) {
            logger.error("Error creating notification", e)
            throw RuntimeException("Error creating notification", e)
        }
    }

    fun createBatch(notifications: List<Notification>): List<Notification> {
        logger.debug("Creating ${notifications.size} notifications in batch")
        return try {
            notificationRepository.saveAll(notifications).toList()
        } catch (e: Exception) {
            logger.error("Error creating batch notifications", e)
            throw RuntimeException("Error creating batch notifications", e)
        }
    }
} 