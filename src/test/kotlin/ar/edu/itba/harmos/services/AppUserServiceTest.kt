package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.PasswordResetToken
import ar.edu.itba.harmos.persistence.AppUserRepository
import ar.edu.itba.harmos.persistence.PasswordResetTokenRepository
import ar.edu.itba.harmos.persistence.RoleRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class AppUserServiceTest {

    @Mock private lateinit var appUserRepository: AppUserRepository
    @Mock private lateinit var roleRepository: RoleRepository
    @Mock private lateinit var passwordEncoder: PasswordEncoder
    @Mock private lateinit var specialtyService: SpecialtyService
    @Mock private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    @Mock private lateinit var emailService: EmailService

    private lateinit var service: AppUserService

    @BeforeEach
    fun setUp() {
        service = AppUserService(
            appUserRepository,
            roleRepository,
            passwordEncoder,
            specialtyService,
            passwordResetTokenRepository,
            emailService,
            "http://localhost:3000"
        )
    }

    private fun createUser(id: Long = 1L) = AppUser(
        email = "user@example.com",
        password = "encoded",
        firstName = "John",
        lastName = "Doe",
        phone = "123",
        roles = mutableSetOf(),
        id = id
    )

    private fun validToken(user: AppUser, expired: Boolean = false): PasswordResetToken {
        val expiry = if (expired) LocalDateTime.now().minusHours(1) else LocalDateTime.now().plusHours(24)
        return PasswordResetToken(user = user, token = UUID.randomUUID().toString(), expiryDate = expiry, id = 1L)
    }

    // ========================= createPasswordResetToken =========================

    @Test
    fun `createPasswordResetToken returns null when user does not exist`() {
        // Given
        `when`(appUserRepository.findByEmail("nobody@example.com")).thenReturn(null)

        // When
        val result = service.createPasswordResetToken("nobody@example.com")

        // Then
        assertThat(result).isNull()
        verify(passwordResetTokenRepository, never()).save(any())
    }

    @Test
    fun `createPasswordResetToken deletes existing token before creating a new one`() {
        // Given
        val user = createUser()
        `when`(appUserRepository.findByEmail(user.email)).thenReturn(user)
        `when`(passwordResetTokenRepository.save(any())).thenAnswer { it.arguments[0] }

        // When
        service.createPasswordResetToken(user.email)

        // Then
        verify(passwordResetTokenRepository).deleteByUserEmail(user.email)
        verify(passwordResetTokenRepository).save(any())
    }

    @Test
    fun `createPasswordResetToken returns a non-blank UUID string`() {
        // Given
        val user = createUser()
        `when`(appUserRepository.findByEmail(user.email)).thenReturn(user)
        `when`(passwordResetTokenRepository.save(any())).thenAnswer { it.arguments[0] }

        // When
        val token = service.createPasswordResetToken(user.email)

        // Then
        assertThat(token).isNotBlank
        assertThat(token).matches("[0-9a-f-]{36}") // UUID format
    }

    // ========================= createPasswordResetTokenForUser =========================

    @Test
    fun `createPasswordResetTokenForUser returns false when user does not exist`() {
        // Given
        `when`(appUserRepository.findByEmail(anyString())).thenReturn(null)

        // When
        val result = service.createPasswordResetTokenForUser("nobody@example.com")

        // Then
        assertThat(result).isFalse
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString())
    }

    @Test
    fun `createPasswordResetTokenForUser sends reset email and returns true when user exists`() {
        // Given
        val user = createUser()
        `when`(appUserRepository.findByEmail(user.email)).thenReturn(user)
        `when`(passwordResetTokenRepository.save(any())).thenAnswer { it.arguments[0] }

        // When
        val result = service.createPasswordResetTokenForUser(user.email)

        // Then
        assertThat(result).isTrue
        verify(emailService).sendPasswordResetEmail(anyString(), anyString())
    }

    // ========================= resetPassword =========================

    @Test
    fun `resetPassword returns false when token is not found`() {
        // Given
        `when`(passwordResetTokenRepository.findByToken("unknown-token")).thenReturn(null)

        // When
        val result = service.resetPassword("unknown-token", "newPassword123")

        // Then
        assertThat(result).isFalse
        verify(appUserRepository, never()).save(any())
    }

    @Test
    fun `resetPassword returns false and deletes token when token is expired`() {
        // Given
        val user = createUser()
        val expiredToken = validToken(user, expired = true)
        `when`(passwordResetTokenRepository.findByToken(expiredToken.token)).thenReturn(expiredToken)

        // When
        val result = service.resetPassword(expiredToken.token, "newPassword123")

        // Then
        assertThat(result).isFalse
        verify(passwordResetTokenRepository).delete(expiredToken)
        verify(appUserRepository, never()).save(any())
    }

    @Test
    fun `resetPassword encodes new password saves user and deletes token on success`() {
        // Given
        val user = createUser()
        val token = validToken(user)
        `when`(passwordResetTokenRepository.findByToken(token.token)).thenReturn(token)
        `when`(passwordEncoder.encode("newPassword123")).thenReturn("encodedNew")
        `when`(appUserRepository.save(any())).thenReturn(user)

        // When
        val result = service.resetPassword(token.token, "newPassword123")

        // Then
        assertThat(result).isTrue
        verify(passwordEncoder).encode("newPassword123")
        verify(appUserRepository).save(user)
        verify(passwordResetTokenRepository).delete(token)
    }

    // ========================= validatePasswordResetToken =========================

    @Test
    fun `validatePasswordResetToken returns false when token is not found`() {
        // Given
        `when`(passwordResetTokenRepository.findByToken("missing")).thenReturn(null)

        // When / Then
        assertThat(service.validatePasswordResetToken("missing")).isFalse
    }

    @Test
    fun `validatePasswordResetToken returns false for expired token`() {
        // Given
        val user = createUser()
        val expired = validToken(user, expired = true)
        `when`(passwordResetTokenRepository.findByToken(expired.token)).thenReturn(expired)

        // When / Then
        assertThat(service.validatePasswordResetToken(expired.token)).isFalse
    }

    @Test
    fun `validatePasswordResetToken returns true for valid non-expired token`() {
        // Given
        val user = createUser()
        val valid = validToken(user, expired = false)
        `when`(passwordResetTokenRepository.findByToken(valid.token)).thenReturn(valid)

        // When / Then
        assertThat(service.validatePasswordResetToken(valid.token)).isTrue
    }
}
