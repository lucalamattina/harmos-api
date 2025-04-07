package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreateAnnouncementRequest
import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse
import ar.edu.itba.harmos.dtos.responses.AppUserResponse
import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.services.AnnouncementService
import ar.edu.itba.harmos.services.AppUserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*


@Validated
@RestController
@RequestMapping("/announcements")
class AnnouncementController(
    private val appUserService: AppUserService,
    private val announcementService: AnnouncementService
) {
    @PostMapping()
    @ResponseBody
    fun create(@RequestBody createAnnouncementRequest: CreateAnnouncementRequest): ResponseEntity<Any> {
        val appUser = appUserService.getAppUserById(createAnnouncementRequest.createdByUserId)
            ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val announcement = announcementService.createAnnouncement(createAnnouncementRequest, appUser)
            ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        return ResponseEntity(AnnouncementResponse.singleFromModel(announcement), HttpStatus.CREATED)
    }


    @GetMapping()
    @ResponseBody
    fun findAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) specialties: List<String>?
    ): ResponseEntity<Any> {
        val announcements = announcementService.searchAnnouncementsByPage(page, size, specialties)
        return ResponseEntity(AnnouncementResponse.listFromModel(announcements), HttpStatus.OK)
    }



    @GetMapping("/{id}")
    @ResponseBody
    fun getById(@PathVariable id: Long): ResponseEntity<Any> {
        val announcement = announcementService.getAnnouncementById(id)
        return if (announcement != null) {
            ResponseEntity(AnnouncementResponse.singleFromModel(announcement), HttpStatus.OK)
        } else ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @PutMapping("/{id}")
    fun updateAnnouncement(
        @PathVariable id: Long,
        @RequestBody announcement: Announcement
    ): ResponseEntity<Announcement> {
        val updatedAnnouncement = announcementService.updateAnnouncement(id, announcement)
        return ResponseEntity.ok(updatedAnnouncement)
    }
}