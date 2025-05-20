package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreatePatientRequest
import ar.edu.itba.harmos.dtos.responses.PatientResponse
import ar.edu.itba.harmos.models.PatientStatus
import ar.edu.itba.harmos.services.PatientService
import ar.edu.itba.harmos.services.AppUserService
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
    private val appUserService: AppUserService
) {

    @GetMapping("/statuses")
    @ResponseBody
    fun getStatuses(): ResponseEntity<Any> {
        return ResponseEntity.ok(PatientStatus.values())
    }

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
            ResponseEntity(PatientResponse.singleFromModel(patient), HttpStatus.OK)
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
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) doctorId: Long?
    ): ResponseEntity<Any> {
        val pageable = PageRequest.of(page, size)
        val patients = when {
            doctorId != null && !name.isNullOrEmpty() -> {
                patientService.getPatientsByDoctorAndName(doctorId, name, pageable)
            }
            !name.isNullOrEmpty() -> {
                patientService.getPatientsContainingName(name, pageable)
            }
            else -> {
                patientService.getPatients(pageable)
            }
        }
        val response = patients.map { PatientResponse.singleFromModel(it) }
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{patientId}/doctors/{doctorId}") //TODO: cambiar Doctors por users?
    fun addDoctorToPatient(@PathVariable patientId: Long, @PathVariable doctorId: Long): ResponseEntity<Any> {
        val patient = patientService.getPatientById(patientId) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val doctor = appUserService.getAppUserById(doctorId) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        
        // Si el doctor ya está asignado, devolvemos NO_CONTENT
        if (patient.doctors.contains(doctor)) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        
        // Si no está asignado, intentamos agregarlo
        return if (patientService.addDoctorToPatient(patientId, doctorId)) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    //TODO: GET DOCTORS DE PACIENTE

    //TODO: GET FILES PACIENTE

}