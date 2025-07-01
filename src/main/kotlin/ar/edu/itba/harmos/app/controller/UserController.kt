package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.responses.AppUserResponse
import ar.edu.itba.harmos.dtos.requests.CreateAppUserRequest
import ar.edu.itba.harmos.dtos.requests.EditAppUserRequest
import ar.edu.itba.harmos.dtos.requests.ForgotPasswordRequest
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
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

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

    @GetMapping("/me")
    @ResponseBody
    fun getCurrentUser(@CurrentUser appUser: AppUser): ResponseEntity<AppUserResponse> {
        return ResponseEntity.ok(AppUserResponse.singleFromModel(appUser))
    }

    @PostMapping()
    @ResponseBody
    fun create(@RequestBody createAppUserRequest: CreateAppUserRequest): ResponseEntity<Any> {
        val appUser = appUserService.createUser(createAppUserRequest)
        return if (appUser != null) {
            ResponseEntity(AppUserResponse.singleFromModel(appUser), HttpStatus.CREATED)
        } else ResponseEntity(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/{id}")
    @ResponseBody
    fun updateUser(
        @PathVariable id: Long,
        @RequestBody editAppUserRequest: EditAppUserRequest
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
        val specialtiesList = specialties?.mapNotNull { specialtyName ->
            specialtyService.getSpecialtyByName(specialtyName)
        }
        val users = appUserService.findAppUsersByEmailAndSpecialties(email, name, specialtiesList, page, size)
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
    fun forgotPassword(@RequestBody forgotPasswordRequest: ForgotPasswordRequest): ResponseEntity<ForgotPasswordResponse> {
        println("=== FORGOT PASSWORD REQUEST ===")
        println("Received email: '${forgotPasswordRequest.email}'")
        println("Email length: ${forgotPasswordRequest.email.length}")
        println("Email trimmed: '${forgotPasswordRequest.email.trim()}'")
        
        // Validar el DTO
        if (!forgotPasswordRequest.isValid()) {
            val error = forgotPasswordRequest.getValidationError() ?: "Datos inválidos"
            println("Validation failed: $error")
            return ResponseEntity(
                ForgotPasswordResponse.validationError(error, forgotPasswordRequest.email), 
                HttpStatus.BAD_REQUEST
            )
        }
        
        println("Email validation passed")

        // Verificar si el usuario existe
        val userExists = appUserService.getAppUserByEmail(forgotPasswordRequest.email.trim()) != null
        println("User exists: $userExists")
        
        if (!userExists) {
            return ResponseEntity(
                ForgotPasswordResponse.error("No se encontró un usuario con este email"), 
                HttpStatus.NOT_FOUND
            )
        }

        // Intentar crear el token de reset
        return try {
            if (appUserService.createPasswordResetTokenForUser(forgotPasswordRequest.email.trim())) {
                println("Password reset token created successfully")
                ResponseEntity(ForgotPasswordResponse.success(), HttpStatus.OK)
            } else {
                println("Failed to create password reset token")
                ResponseEntity(
                    ForgotPasswordResponse.error("No se pudo procesar la solicitud. Inténtalo de nuevo más tarde."), 
                    HttpStatus.INTERNAL_SERVER_ERROR
                )
            }
        } catch (e: Exception) {
            println("Exception creating password reset token: ${e.message}")
            e.printStackTrace()
            ResponseEntity(
                ForgotPasswordResponse.error("Error interno del servidor: ${e.message}"), 
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @PostMapping("/create-reset-token")
    fun createResetToken(@RequestParam email: String): ResponseEntity<Any> {
        val token = appUserService.createPasswordResetToken(email)
        return if (token != null) {
            ResponseEntity.ok(mapOf("token" to token))
        } else {
            ResponseEntity.notFound().build()
        }
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
        @RequestParam token: String,
        @RequestParam newPassword: String
    ): ResponseEntity<Any> {
        return if (appUserService.resetPassword(token, newPassword)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.badRequest().build()
        }
    }

}