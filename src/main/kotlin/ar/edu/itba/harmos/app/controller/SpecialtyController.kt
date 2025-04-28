package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.responses.SpecialtyResponse
import ar.edu.itba.harmos.dtos.requests.CreateSpecialtyRequest
import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse
import ar.edu.itba.harmos.dtos.responses.ScheduleResponse
import ar.edu.itba.harmos.services.AnnouncementService
import ar.edu.itba.harmos.services.SpecialtyService
import ar.edu.itba.harmos.services.ScheduleService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/specialties")
class SpecialtyController(
    private val specialtyService: SpecialtyService,
    private val announcementService: AnnouncementService,
    private val scheduleService: ScheduleService
) {

    @PostMapping()
    @ResponseBody
    fun create(@RequestBody createSpecialtyRequest: CreateSpecialtyRequest): ResponseEntity<Any> {
        val specialty = specialtyService.createSpecialty(createSpecialtyRequest)
        return if (specialty != null) {
            ResponseEntity(SpecialtyResponse.singleFromModel(specialty), HttpStatus.CREATED)
        } else ResponseEntity(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/{id}")
    @ResponseBody
    fun getById(@PathVariable id: Long): ResponseEntity<Any> {
        val specialty = specialtyService.getSpecialtyById(id)
        return if (specialty != null) {
            ResponseEntity(SpecialtyResponse.singleFromModel(specialty), HttpStatus.OK)
        } else ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @GetMapping("/search")
    @ResponseBody
    fun searchByName(@RequestParam name: String): ResponseEntity<Any> {
        val specialty = specialtyService.getSpecialtyByName(name)
        return if (specialty != null) {
            ResponseEntity(SpecialtyResponse.singleFromModel(specialty), HttpStatus.OK)
        } else ResponseEntity(HttpStatus.NOT_FOUND)
    }



    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Any> {
        return if (specialtyService.deletePatientById(id)) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }


}