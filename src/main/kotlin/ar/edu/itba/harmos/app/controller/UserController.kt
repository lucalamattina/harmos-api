package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.responses.AppUserResponse
import ar.edu.itba.harmos.dtos.requests.CreateAppUserRequest
import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse
import ar.edu.itba.harmos.dtos.responses.ScheduleResponse
import ar.edu.itba.harmos.models.AppUser
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

    @PostMapping()
    @ResponseBody
    fun create(@RequestBody createAppUserRequest: CreateAppUserRequest): ResponseEntity<Any> {
        val appUser = appUserService.createUser(createAppUserRequest)
        return if (appUser != null) {
            ResponseEntity(AppUserResponse.singleFromModel(appUser), HttpStatus.CREATED)
        } else ResponseEntity(HttpStatus.BAD_REQUEST)
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
    fun forgotPassword(@RequestParam email: String): ResponseEntity<Any> {
        return if (appUserService.createPasswordResetTokenForUser(email)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
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