package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.exceptions.UnauthorizedNotificationAccessException
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Notification
import ar.edu.itba.harmos.security.resolvers.CurrentUserArgumentResolver
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.NotificationService
import io.jsonwebtoken.Jwts
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime
import java.util.NoSuchElementException

/**
 * Unit tests for [NotificationController] using MockMvc standaloneSetup (no Spring context).
 *
 * ⚠️ Potential bug: POST /notifications has NO authentication check.
 * The [NotificationController.createNotification] endpoint accepts a raw [Notification] body and
 * calls [NotificationService.create] without verifying that the caller is authenticated or is an
 * admin. Any unauthenticated client can POST to /notifications and insert arbitrary records.
 * The other three operations (GET /notifications, GET /notifications/{id},
 * PATCH /notifications/{id}/read) all guard via `@CurrentUser user: AppUser?` + a null check,
 * but createNotification skips both.
 */
@ExtendWith(MockitoExtension::class)
class NotificationControllerTest {

    @Mock private lateinit var notificationService: NotificationService
    @Mock private lateinit var appUserService: AppUserService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val controller = NotificationController(notificationService)
        val resolver = CurrentUserArgumentResolver(appUserService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(resolver)
            .build()
    }

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun createUser(): AppUser =
        AppUser(email = "u@test.com", password = "x", firstName = "A", lastName = "B",
            phone = "0", roles = mutableSetOf(), id = 1L)

    private fun createNotification(user: AppUser, id: Long = 1L, read: Boolean = false): Notification =
        Notification(
            message = "Test notification",
            read = read,
            date = LocalDateTime.of(2024, 1, 1, 12, 0),
            user = user,
            announcementId = null,
            id = id
        )

    private fun setCurrentUser(user: AppUser) {
        val claims = Jwts.claims().setSubject(user.id.toString())
        val auth = UsernamePasswordAuthenticationToken(claims, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
        `when`(appUserService.getAppUserById(user.id)).thenReturn(user)
    }

    // ---------------------------------------------------------------------------
    // Group 1 — GET /notifications
    // ---------------------------------------------------------------------------

    @Test
    fun `getNotifications returns 401 when not authenticated`() {
        // No security context set — CurrentUserArgumentResolver will return null
        mockMvc.perform(get("/notifications"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getNotifications returns all notifications when unreadOnly=false`() {
        val user = createUser()
        setCurrentUser(user)

        val notifications = listOf(
            createNotification(user, id = 1L),
            createNotification(user, id = 2L)
        )
        `when`(notificationService.getAll(user)).thenReturn(notifications)

        mockMvc.perform(get("/notifications").param("unreadOnly", "false"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2))
    }

    @Test
    fun `getNotifications returns only unread when unreadOnly=true`() {
        val user = createUser()
        setCurrentUser(user)

        val unread = listOf(createNotification(user, id = 3L, read = false))
        `when`(notificationService.getUnread(user)).thenReturn(unread)

        mockMvc.perform(get("/notifications").param("unreadOnly", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(3))
            .andExpect(jsonPath("$[0].read").value(false))
    }

    @Test
    fun `getNotifications returns empty list when no notifications exist`() {
        val user = createUser()
        setCurrentUser(user)

        `when`(notificationService.getAll(user)).thenReturn(emptyList())

        mockMvc.perform(get("/notifications"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // ---------------------------------------------------------------------------
    // Group 2 — GET /notifications/{id}
    // ---------------------------------------------------------------------------

    @Test
    fun `getNotification returns 401 when not authenticated`() {
        mockMvc.perform(get("/notifications/1"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getNotification returns 200 with notification when user is owner`() {
        val user = createUser()
        setCurrentUser(user)

        val notification = createNotification(user, id = 1L)
        `when`(notificationService.getById(1L, user)).thenReturn(notification)

        mockMvc.perform(get("/notifications/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.message").value("Test notification"))
            .andExpect(jsonPath("$.read").value(false))
    }

    @Test
    fun `getNotification returns 403 when user is not owner`() {
        val user = createUser()
        setCurrentUser(user)

        `when`(notificationService.getById(1L, user))
            .thenThrow(UnauthorizedNotificationAccessException(1L, 1L))

        mockMvc.perform(get("/notifications/1"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `getNotification returns 404 when notification not found`() {
        val user = createUser()
        setCurrentUser(user)

        `when`(notificationService.getById(99L, user))
            .thenThrow(NoSuchElementException("Notification with ID 99 not found"))

        mockMvc.perform(get("/notifications/99"))
            .andExpect(status().isNotFound)
    }

    // ---------------------------------------------------------------------------
    // Group 3 — PATCH /notifications/{id}/read
    // ---------------------------------------------------------------------------

    @Test
    fun `markAsRead returns 401 when not authenticated`() {
        mockMvc.perform(patch("/notifications/1/read"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `markAsRead returns 200 with updated notification when successful`() {
        val user = createUser()
        setCurrentUser(user)

        val updated = createNotification(user, id = 1L, read = true)
        `when`(notificationService.markAsRead(1L, user)).thenReturn(updated)

        mockMvc.perform(patch("/notifications/1/read"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.read").value(true))
    }

    @Test
    fun `markAsRead returns 403 when user is not owner`() {
        val user = createUser()
        setCurrentUser(user)

        `when`(notificationService.markAsRead(1L, user))
            .thenThrow(UnauthorizedNotificationAccessException(1L, 1L))

        mockMvc.perform(patch("/notifications/1/read"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `markAsRead returns 404 when notification not found`() {
        val user = createUser()
        setCurrentUser(user)

        `when`(notificationService.markAsRead(99L, user))
            .thenThrow(NoSuchElementException("Notification with ID 99 not found"))

        mockMvc.perform(patch("/notifications/99/read"))
            .andExpect(status().isNotFound)
    }
}
