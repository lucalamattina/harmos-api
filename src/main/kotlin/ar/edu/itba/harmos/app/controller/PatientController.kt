package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreatePatientRequest
import ar.edu.itba.harmos.dtos.requests.EditPatientRequest
import ar.edu.itba.harmos.dtos.responses.PatientResponse
import ar.edu.itba.harmos.dtos.responses.ReportResponse
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.AppUserRole
import ar.edu.itba.harmos.models.PatientStatus
import ar.edu.itba.harmos.security.annotations.CurrentUser
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.CloudinaryService
import ar.edu.itba.harmos.services.PatientService
import ar.edu.itba.harmos.services.ReportService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@RequestMapping("/patients")
class PatientController(
    private val patientService: PatientService,
    private val appUserService: AppUserService,
    private val reportService: ReportService,
    private val cloudinaryService: CloudinaryService
) {

    @GetMapping("/statuses")
    @ResponseBody
    fun getStatuses(): ResponseEntity<Any> {
        return ResponseEntity.ok(PatientStatus.values())
    }

    @PostMapping()
    @ResponseBody
    fun create(
        @Valid @RequestBody createPatientRequest: CreatePatientRequest,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        val isAdmin = appUser.roles.any { it.role == AppUserRole.ADMINISTRATOR.roleName }
        if (!isAdmin) {
            return ResponseEntity(mapOf("error" to "Forbidden: only administrators can create patients"), HttpStatus.FORBIDDEN)
        }
        val patient = patientService.createPatient(createPatientRequest)
        return ResponseEntity(PatientResponse.singleFromModel(patient), HttpStatus.CREATED)
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
    fun getPatients(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) doctor: String?,
        @RequestParam(required = false) status: PatientStatus?,
        @RequestParam(required = false) specialty: String?,
        @RequestParam(required = false) doctorId: Long?,
        pageable: Pageable
    ): ResponseEntity<Page<PatientResponse>> {
        val patients = patientService.getPatients(name, doctor, specialty, status, doctorId, pageable)
        return ResponseEntity.ok(patients.map { PatientResponse.singleFromModel(it) })
    }

    @PostMapping("/{patientId}/doctors/{doctorId}")
    fun addDoctorToPatient(@PathVariable patientId: Long, @PathVariable doctorId: Long): ResponseEntity<Any> {
        val patient = patientService.getPatientById(patientId) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val doctor = appUserService.getAppUserById(doctorId) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        
        if (patient.doctors.contains(doctor)) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        
        return if (patientService.addDoctorToPatient(patientId, doctorId)) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PutMapping("/{id}")
    fun updatePatient(
        @PathVariable id: Long,
        @Valid @RequestBody editRequest: EditPatientRequest
    ): ResponseEntity<Any> {
        return try {
            val updatedPatient = patientService.updatePatient(id, editRequest)
            ResponseEntity.ok(PatientResponse.singleFromModel(updatedPatient))
        } catch (ex: RuntimeException) {
            ResponseEntity(mapOf("error" to "Patient not found"), HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/{patientId}/reports")
    @ResponseBody
    fun getPatientReports(
        @PathVariable patientId: Long,
        @RequestParam(required = false) specialtyId: Long?,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        val patient = patientService.getPatientById(patientId)
            ?: return ResponseEntity(mapOf("error" to "Paciente no encontrado"), HttpStatus.NOT_FOUND)

        if (!patient.doctors.contains(appUser)) {
            return ResponseEntity(mapOf("error" to "No tienes acceso a este paciente"), HttpStatus.FORBIDDEN)
        }

        return try {
            val reports = reportService.getReportsForDoctor(appUser, patientId, specialtyId)
            val response = ReportResponse.listFromModel(reports, cloudinaryService)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity(
                mapOf("error" to "Error al obtener reportes del paciente: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
}