package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.exceptions.UnauthorizedNotificationAccessException
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Notification
import ar.edu.itba.harmos.persistence.NotificationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.NoSuchElementException
import java.util.Optional

// ⚠️ Potential bug — markAsRead creates a NEW Notification object (copy) rather than
// mutating the existing entity in-place before saving. While functionally correct for
// returning read=true, it means the JPA entity being saved is detached from the original
// managed instance. In a real @Transactional context this is harmless because the ID is
// preserved and Hibernate will issue an UPDATE, but it is an unusual pattern that
// deviates from idiomatic JPA (update-the-managed-entity, let flush save it). If the
// entity gains more fields in the future, the manual copy block will silently drop them.
// Consider using a mutable `var read` field and updating it in-place instead.

@ExtendWith(MockitoExtension::class)
class NotificationServiceTest {

    @Mock private lateinit var notificationRepository: NotificationRepository

    private lateinit var service: NotificationService

    @BeforeEach
    fun setUp() {
        service = NotificationService(notificationRepository)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun createUser(id: Long = 1L) = AppUser(
        email = "u@test.com",
        password = "x",
        firstName = "A",
        lastName = "B",
        phone = "0",
        roles = mutableSetOf(),
        id = id
    )

    private fun createNotification(
        id: Long = 10L,
        user: AppUser = createUser(),
        read: Boolean = false,
        message: String = "Test notification"
    ) = Notification(
        message = message,
        read = read,
        date = LocalDateTime.of(2024, 1, 15, 10, 0),
        user = user,
        announcementId = null,
        reportId = null,
        createdAt = LocalDateTime.of(2024, 1, 15, 10, 0),
        updatedAt = LocalDateTime.of(2024, 1, 15, 10, 0),
        id = id
    )

    // ------------------------------------------------------------------
    // Group 1 — getById
    // ------------------------------------------------------------------

    @Test
    fun `getById returns notification when user is the owner`() {
        // Given
        val user = createUser(id = 1L)
        val notification = createNotification(id = 10L, user = user)
        `when`(notificationRepository.findById(10L)).thenReturn(Optional.of(notification))

        // When
        val result = service.getById(10L, user)

        // Then
        assertThat(result).isEqualTo(notification)
        verify(notificationRepository).findById(10L)
    }

    @Test
    fun `getById throws NoSuchElementException when notification not found`() {
        // Given
        `when`(notificationRepository.findById(99L)).thenReturn(Optional.empty())

        // When / Then
        assertThrows<NoSuchElementException> {
            service.getById(99L, createUser())
        }
    }

    @Test
    fun `getById throws UnauthorizedNotificationAccessException when notification belongs to different user`() {
        // Given — notification owned by user with id=2, caller has id=1
        val owner = createUser(id = 2L)
        val caller = createUser(id = 1L)
        val notification = createNotification(id = 10L, user = owner)
        `when`(notificationRepository.findById(10L)).thenReturn(Optional.of(notification))

        // When / Then
        assertThrows<UnauthorizedNotificationAccessException> {
            service.getById(10L, caller)
        }
    }

    // ------------------------------------------------------------------
    // Group 2 — markAsRead
    // ------------------------------------------------------------------

    @Test
    fun `markAsRead returns notification unchanged when already read`() {
        // Given — notification already has read=true
        val user = createUser(id = 1L)
        val notification = createNotification(id = 10L, user = user, read = true)
        `when`(notificationRepository.findById(10L)).thenReturn(Optional.of(notification))

        // When
        val result = service.markAsRead(10L, user)

        // Then — early return: save must NOT be called
        assertThat(result).isEqualTo(notification)
        assertThat(result.read).isTrue
        verify(notificationRepository, never()).save(any())
    }

    @Test
    fun `markAsRead saves updated notification with read=true when not yet read`() {
        // Given — notification has read=false
        val user = createUser(id = 1L)
        val notification = createNotification(id = 10L, user = user, read = false)
        `when`(notificationRepository.findById(10L)).thenReturn(Optional.of(notification))

        // The service builds a new Notification copy with read=true and saves it.
        // We capture that argument so we can assert its read flag.
        val savedCaptor = org.mockito.ArgumentCaptor.forClass(Notification::class.java)
        val savedNotification = createNotification(id = 10L, user = user, read = true)
        `when`(notificationRepository.save(any(Notification::class.java))).thenReturn(savedNotification)

        // When
        val result = service.markAsRead(10L, user)

        // Then
        verify(notificationRepository).save(savedCaptor.capture())
        assertThat(savedCaptor.value.read).isTrue
        assertThat(savedCaptor.value.id).isEqualTo(10L)
        assertThat(result.read).isTrue
    }

    @Test
    fun `markAsRead throws UnauthorizedNotificationAccessException when wrong user tries to mark as read`() {
        // Given — notification owned by user id=2, request from user id=1
        val owner = createUser(id = 2L)
        val caller = createUser(id = 1L)
        val notification = createNotification(id = 10L, user = owner, read = false)
        `when`(notificationRepository.findById(10L)).thenReturn(Optional.of(notification))

        // When / Then
        assertThrows<UnauthorizedNotificationAccessException> {
            service.markAsRead(10L, caller)
        }

        // Save must never be reached
        verify(notificationRepository, never()).save(any())
    }

    // ------------------------------------------------------------------
    // Group 3 — getAll / getUnread
    // ------------------------------------------------------------------

    @Test
    fun `getAll delegates to repository and returns list`() {
        // Given
        val user = createUser()
        val notifications = listOf(
            createNotification(id = 1L, user = user, read = false),
            createNotification(id = 2L, user = user, read = true)
        )
        `when`(notificationRepository.findByUserOrderByDateDesc(user)).thenReturn(notifications)

        // When
        val result = service.getAll(user)

        // Then
        assertThat(result).isEqualTo(notifications)
        assertThat(result).hasSize(2)
        verify(notificationRepository).findByUserOrderByDateDesc(user)
    }

    @Test
    fun `getUnread delegates to repository filtered list`() {
        // Given
        val user = createUser()
        val unread = listOf(createNotification(id = 3L, user = user, read = false))
        `when`(notificationRepository.findByUserAndReadFalseOrderByDateDesc(user)).thenReturn(unread)

        // When
        val result = service.getUnread(user)

        // Then
        assertThat(result).isEqualTo(unread)
        assertThat(result).allMatch { !it.read }
        verify(notificationRepository).findByUserAndReadFalseOrderByDateDesc(user)
    }

    // ------------------------------------------------------------------
    // Group 4 — create / createBatch
    // ------------------------------------------------------------------

    @Test
    fun `create delegates to repository save`() {
        // Given
        val user = createUser()
        val notification = createNotification(id = -1L, user = user)
        val persisted = createNotification(id = 42L, user = user)
        `when`(notificationRepository.save(notification)).thenReturn(persisted)

        // When
        val result = service.create(notification)

        // Then
        assertThat(result).isEqualTo(persisted)
        assertThat(result.id).isEqualTo(42L)
        verify(notificationRepository).save(notification)
    }

    @Test
    fun `createBatch delegates to repository saveAll`() {
        // Given
        val user = createUser()
        val batch = listOf(
            createNotification(id = -1L, user = user, message = "Msg 1"),
            createNotification(id = -1L, user = user, message = "Msg 2")
        )
        val persisted = listOf(
            createNotification(id = 11L, user = user, message = "Msg 1"),
            createNotification(id = 12L, user = user, message = "Msg 2")
        )
        `when`(notificationRepository.saveAll(batch)).thenReturn(persisted)

        // When
        val result = service.createBatch(batch)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.id }).containsExactly(11L, 12L)
        verify(notificationRepository).saveAll(batch)
    }
}
