package ar.edu.itba.harmos.dtos.requests

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ForgotPasswordRequestTest {

    // ===================== isValid =====================

    @Test
    fun `isValid returns true for well-formed email`() {
        assertThat(ForgotPasswordRequest("user@example.com").isValid()).isTrue
    }

    @Test
    fun `isValid returns false for blank email`() {
        assertThat(ForgotPasswordRequest("").isValid()).isFalse
        assertThat(ForgotPasswordRequest("   ").isValid()).isFalse
    }

    @Test
    fun `isValid returns false for email missing at-sign`() {
        assertThat(ForgotPasswordRequest("userdomain.com").isValid()).isFalse
    }

    @Test
    fun `isValid returns false for email missing domain dot`() {
        assertThat(ForgotPasswordRequest("user@domain").isValid()).isFalse
    }

    @Test
    fun `isValid returns false for email with equals sign`() {
        assertThat(ForgotPasswordRequest("user=x@domain.com").isValid()).isFalse
    }

    // ===================== getValidationError =====================

    @Test
    fun `getValidationError returns null for structurally valid email`() {
        assertThat(ForgotPasswordRequest("user@example.com").getValidationError()).isNull()
    }

    @Test
    fun `getValidationError returns error for blank email`() {
        assertThat(ForgotPasswordRequest("").getValidationError()).isNotNull
    }

    @Test
    fun `getValidationError returns error for email missing at-sign`() {
        assertThat(ForgotPasswordRequest("userdomain.com").getValidationError()).isNotNull
    }

    @Test
    fun `getValidationError returns error for email starting with at-sign`() {
        assertThat(ForgotPasswordRequest("@domain.com").getValidationError()).isNotNull
    }

    @Test
    fun `getValidationError returns error for email ending with at-sign`() {
        assertThat(ForgotPasswordRequest("user@").getValidationError()).isNotNull
    }

    @Test
    fun `getValidationError returns error for email with equals sign`() {
        assertThat(ForgotPasswordRequest("user=x@domain.com").getValidationError()).isNotNull
    }

    // ===================== consistency (F-03 resolved) =====================

    @Test
    fun `isValid and getValidationError are consistent — isValid is true iff getValidationError is null`() {
        val cases = listOf(
            "user@example.com",    // valid
            "user@test.c",         // single-char TLD — both now accept (structural check only)
            "",                    // blank — both reject
            "nodomain",            // missing @ — both reject
            "user=x@domain.com"    // equals sign — both reject
        )
        cases.forEach { email ->
            val req = ForgotPasswordRequest(email)
            assertThat(req.isValid())
                .`as`("isValid for '$email' should equal (getValidationError == null)")
                .isEqualTo(req.getValidationError() == null)
        }
    }
}
