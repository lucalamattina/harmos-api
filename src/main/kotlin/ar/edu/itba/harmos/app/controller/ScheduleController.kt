package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreateScheduleRequest
import ar.edu.itba.harmos.dtos.responses.ScheduleResponse
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.LocationService
import ar.edu.itba.harmos.services.PatientService
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
        private val scheduleService: ScheduleService,
        private val patientService: PatientService,
        private val locationService: LocationService
) {
        @PostMapping()
        @ResponseBody
        fun create(@RequestBody createScheduleRequest: CreateScheduleRequest): ResponseEntity<Any> {
                val appUser =
                        appUserService.getAppUserById(createScheduleRequest.doctorUserId)
                                ?: return ResponseEntity(
                                        mapOf("error" to "Doctor not found"),
                                        HttpStatus.NOT_FOUND
                                )
                val patient =
                        patientService.getPatientById(createScheduleRequest.patientId)
                                ?: return ResponseEntity(
                                        mapOf("error" to "Patient not found"),
                                        HttpStatus.NOT_FOUND
                                )
                val location =
                        locationService.getLocationById(createScheduleRequest.locationId)
                                ?: return ResponseEntity(
                                        mapOf("error" to "Location not found"),
                                        HttpStatus.NOT_FOUND
                                )

                val schedule =
                        scheduleService.createSchedule(
                                createScheduleRequest,
                                appUser,
                                patient,
                                location
                        )
                return ResponseEntity(
                        ScheduleResponse.singleFromModel(schedule),
                        HttpStatus.CREATED
                )
        }

        @GetMapping()
        @ResponseBody
        fun findAll(
                @RequestParam(name = "locationId", required = false) locationId: Long?,
                @RequestParam(name = "doctorId", required = false) doctorId: Long?,
                @RequestParam(name = "patientId", required = false) patientId: Long?
        ): ResponseEntity<Any> {

                val schedules =
                        scheduleService.findFilteredSchedules(locationId, doctorId, patientId)
                return ResponseEntity(ScheduleResponse.setFromModel(schedules), HttpStatus.OK)
        }

        @DeleteMapping("/{id}")
        fun deleteSchedule(@PathVariable id: Long): ResponseEntity<Any> {
                return if (scheduleService.deleteScheduleById(id)) {
                        ResponseEntity.noContent().build()
                } else {
                        ResponseEntity.notFound().build()
                }
        }
}
