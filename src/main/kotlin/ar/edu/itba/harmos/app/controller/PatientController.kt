package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreatePatientRequest
import ar.edu.itba.harmos.dtos.responses.PatientResponse
import ar.edu.itba.harmos.dtos.responses.PatientDetailResponse
import ar.edu.itba.harmos.services.PatientService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/patients")
class PatientController(
    private val patientService: PatientService,
) {

    @PostMapping()
    @ResponseBody
    fun create(@RequestBody createPatientRequest: CreatePatientRequest): ResponseEntity<Any> {
        val patient = patientService.createPatient(createPatientRequest)
        return if (patient != null) {
            ResponseEntity(PatientResponse.singleFromModel(patient), HttpStatus.CREATED)
        } else ResponseEntity(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/{id}")
    @ResponseBody
    fun getById(@PathVariable id: Long): ResponseEntity<Any> {
        val patient = patientService.getPatientById(id)
        return if (patient != null) {
            ResponseEntity(PatientDetailResponse.fromModel(patient), HttpStatus.OK)
        } else ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Any> {
        return if (patientService.deletePatientById(id)) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping
    @ResponseBody
    fun getPatients(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) name: String?
    ): ResponseEntity<Any> {
        val pageable = PageRequest.of(page, size)
        val patients = if (!name.isNullOrEmpty()) {
            patientService.getPatientsContainingName(name, pageable)
        } else {
            patientService.getPatients(pageable)
        }
        return ResponseEntity.ok(PatientResponse.pageFromModel(patients))
    }


    @PostMapping("/{patientId}/doctors/{doctorId}") //TODO: cambiar Doctors por users?
    fun addDoctorToPatient(@PathVariable patientId: Long, @PathVariable doctorId: Long): ResponseEntity<Any> {
        return if (patientService.addDoctorToPatient(patientId, doctorId)) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    //TODO: GET DOCTORS DE PACIENTE

    //TODO: GET FILES PACIENTE

}