package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.models.Notification
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.services.NotificationService
import ar.edu.itba.harmos.security.annotations.CurrentUser
import ar.edu.itba.harmos.exceptions.UnauthorizedNotificationAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ar.edu.itba.harmos.dtos.responses.NotificationResponse
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {
    private val logger = LoggerFactory.getLogger(NotificationController::class.java)

    @GetMapping
    fun getNotifications(
        @CurrentUser user: AppUser?,
        @RequestParam(required = false, defaultValue = "false") unreadOnly: Boolean
    ): ResponseEntity<List<NotificationResponse>> {
        if (user == null) {
            logger.warn("Attempt to access without authenticated user")
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        
        return try {
            logger.debug("Getting notifications for user ${user.id}, unreadOnly: $unreadOnly")
            val notifications = if (unreadOnly)
                notificationService.getUnread(user)
            else
                notificationService.getAll(user)
            val response = notifications.map { NotificationResponse(it.id, it.message, it.read, it.date, it.announcementId) }
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error getting notifications for user ${user.id}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/{id}")
    fun getNotification(
        @PathVariable id: Long,
        @CurrentUser user: AppUser?
    ): ResponseEntity<Any> {
        if (user == null) {
            logger.warn("Attempt to access without authenticated user")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "User not authenticated"))
        }

        return try {
            val notification = notificationService.getById(id, user)
            val response = NotificationResponse(notification.id, notification.message, notification.read, notification.date, notification.announcementId)
            ResponseEntity.ok(response)
        } catch (e: UnauthorizedNotificationAccessException) {
            logger.warn("Access denied: ${e.message}")
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: NoSuchElementException) {
            logger.warn("Notification not found: $id")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Notification not found"))
        } catch (e: Exception) {
            logger.error("Error getting notification $id", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Internal server error"))
        }
    }

    @PatchMapping("/{id}/read")
    fun markAsRead(
        @PathVariable id: Long,
        @CurrentUser user: AppUser?
    ): ResponseEntity<Any> {
        logger.debug("Attempt to mark notification $id as read")
        
        if (user == null) {
            logger.warn("Attempt to mark notification as read without authenticated user")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "User not authenticated"))
        }
        
        return try {
            logger.debug("User ${user.id} attempting to mark notification $id as read")
            val notification = notificationService.markAsRead(id, user)
            val response = NotificationResponse(notification.id, notification.message, notification.read, notification.date, notification.announcementId)
            ResponseEntity.ok(response)
        } catch (e: UnauthorizedNotificationAccessException) {
            logger.warn("Access denied: ${e.message}")
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: NoSuchElementException) {
            logger.warn("Notification not found: $id", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Notification not found"))
        } catch (e: Exception) {
            logger.error("Unexpected error marking notification $id as read", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Internal server error"))
        }
    }

    @PostMapping
    fun createNotification(@RequestBody notification: Notification): ResponseEntity<Any> {
        return try {
            logger.debug("Creating new notification")
            val created = notificationService.create(notification)
            val response = NotificationResponse(created.id, created.message, created.read, created.date, created.announcementId)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            logger.error("Error creating notification", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Error creating notification"))
        }
    }
} 