package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.responses.AppUserResponse
import ar.edu.itba.harmos.dtos.requests.CreateAppUserRequest
import ar.edu.itba.harmos.dtos.requests.EditAppUserRequest
import ar.edu.itba.harmos.dtos.requests.ForgotPasswordRequest
import ar.edu.itba.harmos.dtos.requests.ResetPasswordRequest
import ar.edu.itba.harmos.dtos.responses.ForgotPasswordResponse
import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse
import ar.edu.itba.harmos.dtos.responses.ScheduleResponse
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.security.annotations.CurrentUser
import ar.edu.itba.harmos.services.AnnouncementService
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.ScheduleService
import ar.edu.itba.harmos.services.SpecialtyService
import ar.edu.itba.harmos.persistence.PasswordResetTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@RequestMapping("/users")
class UserController(
    private val appUserService: AppUserService,
    private val announcementService: AnnouncementService,
    private val scheduleService: ScheduleService,
    private val specialtyService: SpecialtyService,
    private val passwordResetTokenRepository: PasswordResetTokenRepository
) {

    private val logger = LoggerFactory.getLogger(UserController::class.java)

    @GetMapping("/me")
    @ResponseBody
    fun getCurrentUser(@CurrentUser appUser: AppUser): ResponseEntity<AppUserResponse> {
        return ResponseEntity.ok(AppUserResponse.singleFromModel(appUser))
    }

    @PostMapping()
    @ResponseBody
    fun create(@Valid @RequestBody createAppUserRequest: CreateAppUserRequest): ResponseEntity<Any> {
        return try {
            val appUser = appUserService.createUser(createAppUserRequest)
            ResponseEntity(AppUserResponse.singleFromModel(appUser), HttpStatus.CREATED)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.BAD_REQUEST)
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody editAppUserRequest: EditAppUserRequest
    ): ResponseEntity<Any> {
        val updatedUser = appUserService.updateUser(id, editAppUserRequest)
        return if (updatedUser != null) {
            ResponseEntity(AppUserResponse.singleFromModel(updatedUser), HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/{id}")
    @ResponseBody
    fun getById(@PathVariable id: Long): ResponseEntity<Any> {
        val appUser = appUserService.getAppUserById(id)
        return if (appUser != null) {
            ResponseEntity(AppUserResponse.singleFromModel(appUser), HttpStatus.OK)
        } else ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @GetMapping("/{id}/announcements")
    @ResponseBody
    fun findAnnouncementsById(@PathVariable id: Long, @RequestParam page: Int, @RequestParam offset: Int): ResponseEntity<Any> {
        val appUser = appUserService.getAppUserById(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val announcements = announcementService.searchAnnouncementsByCreatedByAndPage(appUser, page, offset)
        return ResponseEntity(AnnouncementResponse.listFromModel(announcements), HttpStatus.OK)
    }

    @GetMapping("/{id}/schedules")
    @ResponseBody
    fun getScheduleById(@PathVariable id: Long): ResponseEntity<Any> {
        val appUser = appUserService.getAppUserById(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val schedules = scheduleService.findAllByDoctor(appUser)
        return ResponseEntity(ScheduleResponse.setFromModel(schedules), HttpStatus.OK)
    }

    @GetMapping
    fun getUsers(
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) specialties: List<String>?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<AppUserResponse>> {
        val users = appUserService.findAppUsersByEmailAndSpecialties(email, name, specialties, page, size)
        return ResponseEntity.ok(users.map { AppUserResponse.singleFromModel(it) })
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Any> {
        return if (appUserService.deleteUserById(id)) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/{userId}/specialties/{specialtyId}")
    fun addSpecialtyToUser(
        @PathVariable userId: Long,
        @PathVariable specialtyId: Long
    ): ResponseEntity<Any> {
        return if (appUserService.addSpecialtyToUser(userId, specialtyId)) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@RequestBody forgotPasswordRequest: ForgotPasswordRequest): ResponseEntity<Any> {
        if (!forgotPasswordRequest.isValid()) {
            val error = forgotPasswordRequest.getValidationError() ?: "Datos inválidos"
            logger.warn("Forgot-password validation failed: {}", error)
            return ResponseEntity(
                mapOf("message" to "If the email is registered, a reset link will be sent"),
                HttpStatus.OK
            )
        }

        logger.debug("Forgot-password request received")

        // Attempt to create the reset token; do NOT reveal whether the user exists
        return try {
            appUserService.createPasswordResetTokenForUser(forgotPasswordRequest.email.trim())
            logger.info("Forgot-password flow completed for request")
            ResponseEntity(
                mapOf("message" to "If the email is registered, a reset link will be sent"),
                HttpStatus.OK
            )
        } catch (e: Exception) {
            logger.error("Error processing forgot-password request", e)
            ResponseEntity(
                mapOf("message" to "If the email is registered, a reset link will be sent"),
                HttpStatus.OK
            )
        }
    }

    @PostMapping("/create-reset-token")
    fun createResetToken(@RequestParam email: String): ResponseEntity<Any> {
        appUserService.createPasswordResetTokenForUser(email)
        return ResponseEntity(mapOf("message" to "Password reset email sent"), HttpStatus.OK)
    }

    @GetMapping("/validate-reset-token")
    fun validateResetToken(@RequestParam token: String): ResponseEntity<Any> {
        val resetToken = passwordResetTokenRepository.findByToken(token)
        return when {
            resetToken == null -> ResponseEntity.notFound().build()
            resetToken.isExpired() -> ResponseEntity.status(HttpStatus.GONE).build()
            else -> ResponseEntity.ok().build()
        }
    }

    @PostMapping("/reset-password")
    fun resetPassword(
        @Valid @RequestBody resetPasswordRequest: ResetPasswordRequest
    ): ResponseEntity<Any> {
        return if (appUserService.resetPassword(resetPasswordRequest.token, resetPasswordRequest.newPassword)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.badRequest().build()
        }
    }

}