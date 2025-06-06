package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.models.Notification
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.persistence.NotificationRepository
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
        return notificationRepository.findByUserAndReadFalse(user)
    }

    fun getAll(user: AppUser): List<Notification> {
        logger.debug("Finding all notifications for user ${user.id}")
        return notificationRepository.findByUser(user)
    }

    @Transactional
    fun markAsRead(id: Long, user: AppUser): Notification {
        logger.debug("Attempting to mark notification $id as read for user ${user.id}")
        
        val notification = try {
            notificationRepository.findById(id).orElseThrow {
                NoSuchElementException("Notification with ID $id not found")
            }
        } catch (e: Exception) {
            logger.error("Error finding notification $id", e)
            throw e
        }
        
        if (notification.user.id != user.id) {
            logger.warn("User ${user.id} attempted to access notification ${notification.id} which belongs to user ${notification.user.id}")
            throw IllegalArgumentException("Notification $id belongs to another user")
        }

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
} 