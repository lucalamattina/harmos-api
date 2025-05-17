package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreateAnnouncementRequest
import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse
import ar.edu.itba.harmos.dtos.responses.AppUserResponse
import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.security.annotations.CurrentUser
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
    private val announcementService: AnnouncementService
) {
    @PostMapping()
    @ResponseBody
    fun create(
        @RequestBody createAnnouncementRequest: CreateAnnouncementRequest,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }

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
        val response = announcements.map { AnnouncementResponse.singleFromModel(it) }
        return ResponseEntity.ok(response)
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