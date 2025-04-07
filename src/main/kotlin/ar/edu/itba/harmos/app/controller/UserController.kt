package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.responses.AppUserResponse
import ar.edu.itba.harmos.dtos.requests.CreateAppUserRequest
import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse
import ar.edu.itba.harmos.dtos.responses.ScheduleResponse
import ar.edu.itba.harmos.services.AnnouncementService
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.ScheduleService
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
    private val scheduleService: ScheduleService
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

    //TODO: GET ALL USERS(ADMIN PAGE) PAgearlo
    //TODO: DELETE USER
    @GetMapping()
    @ResponseBody
    fun findAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) specialties: List<String>?,
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) id: Long?
    ): ResponseEntity<Any> {
        val users = when {
            !specialties.isNullOrEmpty() && (id != null || email != null) ->
                appUserService.findUserBySpecialtyAndIdOrEmail(specialties, id, email)
            !specialties.isNullOrEmpty() ->
                appUserService.findUsersBySpecialties(specialties, page, size)
            id != null || email != null ->
                appUserService.findUserByIdOrEmail(id, email)
            else -> appUserService.findAllUsers(page, size)
        }
        return ResponseEntity(AppUserResponse.listFromModel(users), HttpStatus.OK)
    }


}