package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreateScheduleRequest
import ar.edu.itba.harmos.dtos.responses.ScheduleResponse
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.ScheduleService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/schedules")
class ScheduleController(
    private val appUserService: AppUserService,
    private val scheduleService: ScheduleService
) {
    @PostMapping()
    @ResponseBody
    fun create(@RequestBody createScheduleRequest: CreateScheduleRequest): ResponseEntity<Any> {
        val appUser = appUserService.getAppUserById(createScheduleRequest.doctorUserId)
            ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val schedule = scheduleService.createSchedule(createScheduleRequest, appUser)
            ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        return ResponseEntity(ScheduleResponse.singleFromModel(schedule), HttpStatus.CREATED)
    }

    @GetMapping()
    @ResponseBody
    fun findAll(): ResponseEntity<Any> {
        val schedules = scheduleService.findAll()
        return ResponseEntity(ScheduleResponse.setFromModel(schedules), HttpStatus.OK)
    }
}