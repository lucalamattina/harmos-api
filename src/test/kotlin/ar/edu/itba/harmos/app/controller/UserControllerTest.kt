package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.PasswordResetToken
import ar.edu.itba.harmos.persistence.PasswordResetTokenRepository
import ar.edu.itba.harmos.services.AnnouncementService
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.ScheduleService
import ar.edu.itba.harmos.services.SpecialtyService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class UserControllerTest {

    @Mock private lateinit var appUserService: AppUserService
    @Mock private lateinit var announcementService: AnnouncementService
    @Mock private lateinit var scheduleService: ScheduleService
    @Mock private lateinit var specialtyService: SpecialtyService
    @Mock private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val controller = UserController(
            appUserService,
            announcementService,
            scheduleService,
            specialtyService,
            passwordResetTokenRepository
        )
        // Standalone setup: no Spring context, no security filters, full controller logic
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    private fun createUser() = AppUser(
        email = "user@example.com", password = "x", firstName = "A", lastName = "B",
        phone = "0", roles = mutableSetOf(), id = 1L
    )

    private fun validToken(user: AppUser, expired: Boolean = false): PasswordResetToken {
        val expiry = if (expired) LocalDateTime.now().minusHours(1) else LocalDateTime.now().plusHours(24)
        return PasswordResetToken(user = user, token = "some-token", expiryDate = expiry, id = 1L)
    }

    // ========================= forgot-password user-existence oracle =========================

    @Test
    fun `forgotPassword returns 200 with generic message when user does not exist`() {
        `when`(appUserService.createPasswordResetTokenForUser("nobody@example.com")).thenReturn(false)

        mockMvc.perform(
            post("/users/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"nobody@example.com"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("If the email is registered, a reset link will be sent"))
    }

    @Test
    fun `forgotPassword returns 200 with same generic message when user exists`() {
        `when`(appUserService.createPasswordResetTokenForUser("user@example.com")).thenReturn(true)

        mockMvc.perform(
            post("/users/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("If the email is registered, a reset link will be sent"))
    }

    @Test
    fun `forgotPassword returns 200 with generic message for invalid email format (no oracle leak)`() {
        mockMvc.perform(
            post("/users/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"not-an-email"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("If the email is registered, a reset link will be sent"))
    }

    @Test
    fun `forgotPassword returns 200 with generic message even when service throws`() {
        `when`(appUserService.createPasswordResetTokenForUser(anyString()))
            .thenThrow(RuntimeException("unexpected"))

        mockMvc.perform(
            post("/users/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("If the email is registered, a reset link will be sent"))
    }

    // ========================= validate-reset-token =========================

    @Test
    fun `validateResetToken returns 404 when token does not exist`() {
        `when`(passwordResetTokenRepository.findByToken("missing")).thenReturn(null)

        mockMvc.perform(get("/users/validate-reset-token").param("token", "missing"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `validateResetToken returns 410 GONE when token is expired`() {
        val user = createUser()
        val expired = validToken(user, expired = true)
        `when`(passwordResetTokenRepository.findByToken("some-token")).thenReturn(expired)

        mockMvc.perform(get("/users/validate-reset-token").param("token", "some-token"))
            .andExpect(status().isGone)
    }

    @Test
    fun `validateResetToken returns 200 for valid non-expired token`() {
        val user = createUser()
        val valid = validToken(user, expired = false)
        `when`(passwordResetTokenRepository.findByToken("some-token")).thenReturn(valid)

        mockMvc.perform(get("/users/validate-reset-token").param("token", "some-token"))
            .andExpect(status().isOk)
    }

    // ========================= reset-password =========================

    @Test
    fun `resetPassword returns 200 when token is valid and password is reset`() {
        `when`(appUserService.resetPassword("good-token", "newPass1234")).thenReturn(true)

        mockMvc.perform(
            post("/users/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"good-token","newPassword":"newPass1234"}""")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `resetPassword returns 400 when token is invalid or expired`() {
        `when`(appUserService.resetPassword("bad-token", "newPass1234")).thenReturn(false)

        mockMvc.perform(
            post("/users/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"bad-token","newPassword":"newPass1234"}""")
        )
            .andExpect(status().isBadRequest)
    }
}
