package ar.edu.itba.harmos.models

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PasswordResetTokenTest {

    private val testUser = AppUser(
        email = "u@test.com",
        password = "x",
        firstName = "A",
        lastName = "B",
        phone = "0",
        roles = mutableSetOf()
    )

    @Test
    fun `isExpired returns false when expiryDate is in the future`() {
        // Given
        val token = PasswordResetToken(
            user = testUser,
            token = "some-token",
            expiryDate = LocalDateTime.now().plusHours(1)
        )

        // When / Then
        assertThat(token.isExpired()).isFalse
    }

    @Test
    fun `isExpired returns true when expiryDate is in the past`() {
        // Given
        val token = PasswordResetToken(
            user = testUser,
            token = "some-token",
            expiryDate = LocalDateTime.now().minusHours(1)
        )

        // When / Then
        assertThat(token.isExpired()).isTrue
    }

    @Test
    fun `isExpired returns true when expiryDate is exactly now minus 1 second`() {
        // Given
        val token = PasswordResetToken(
            user = testUser,
            token = "some-token",
            expiryDate = LocalDateTime.now().minusSeconds(1)
        )

        // When / Then
        assertThat(token.isExpired()).isTrue
    }

    @Test
    fun `isExpired returns false when expiryDate is exactly now plus 1 second`() {
        // Given
        val token = PasswordResetToken(
            user = testUser,
            token = "some-token",
            expiryDate = LocalDateTime.now().plusSeconds(1)
        )

        // When / Then
        assertThat(token.isExpired()).isFalse
    }
}
