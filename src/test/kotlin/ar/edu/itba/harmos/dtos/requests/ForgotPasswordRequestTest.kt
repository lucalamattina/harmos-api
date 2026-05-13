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

    // ===================== known inconsistency =====================

    /**
     * Bug #3: isValid() applies a stricter TLD regex than getValidationError().
     * An email with a single-char TLD passes all getValidationError() checks (returns null)
     * but fails isValid() (returns false). Both code paths in the controller return the
     * same 200 response, so this is safe at runtime but a maintenance hazard.
     */
    @Test
    fun `isValid and getValidationError disagree on single-char TLD (known inconsistency)`() {
        val request = ForgotPasswordRequest("user@test.c")
        // getValidationError sees no structural problem
        assertThat(request.getValidationError()).isNull()
        // but isValid applies the stricter regex and rejects it
        assertThat(request.isValid()).isFalse
    }
}
