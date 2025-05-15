package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.models.Specialty
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.PatientService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@WebMvcTest(PatientController::class)
class PatientControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var patientService: PatientService

    @MockBean
    private lateinit var appUserService: AppUserService

    @Test
    fun `addDoctorToPatient should return NO_CONTENT when doctor is successfully added`() {
        // Given
        val patientId = 1L
        val doctorId = 1L
        val patient = Patient("Test Patient", "123", "ACTIVE", mutableListOf(), emptyList(), patientId)
        val doctor = AppUser("test@test.com", "pass", "John", "Doe", "123", mutableSetOf(Specialty("TO")), emptySet(), doctorId)

        `when`(patientService.getPatientById(patientId)).thenReturn(patient)
        `when`(appUserService.getAppUserById(doctorId)).thenReturn(doctor)
        `when`(patientService.addDoctorToPatient(patientId, doctorId)).thenReturn(true)

        // When/Then
        mockMvc.perform(
            post("/patients/$patientId/doctors/$doctorId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `addDoctorToPatient should return NO_CONTENT when doctor is already assigned`() {
        // Given
        val patientId = 1L
        val doctorId = 1L
        val doctor = AppUser("test@test.com", "pass", "John", "Doe", "123", mutableSetOf(Specialty("TO")), emptySet(), doctorId)
        val patient = Patient("Test Patient", "123", "ACTIVE", mutableListOf(doctor), emptyList(), patientId)

        `when`(patientService.getPatientById(patientId)).thenReturn(patient)
        `when`(appUserService.getAppUserById(doctorId)).thenReturn(doctor)

        // When/Then
        mockMvc.perform(
            post("/patients/$patientId/doctors/$doctorId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `addDoctorToPatient should return NOT_FOUND when patient does not exist`() {
        // Given
        val patientId = 1L
        val doctorId = 1L

        `when`(patientService.getPatientById(patientId)).thenReturn(null)

        // When/Then
        mockMvc.perform(
            post("/patients/$patientId/doctors/$doctorId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `addDoctorToPatient should return NOT_FOUND when doctor does not exist`() {
        // Given
        val patientId = 1L
        val doctorId = 1L
        val patient = Patient("Test Patient", "123", "ACTIVE", mutableListOf(), emptyList(), patientId)

        `when`(patientService.getPatientById(patientId)).thenReturn(patient)
        `when`(appUserService.getAppUserById(doctorId)).thenReturn(null)

        // When/Then
        mockMvc.perform(
            post("/patients/$patientId/doctors/$doctorId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `addDoctorToPatient should return INTERNAL_SERVER_ERROR when service fails`() {
        // Given
        val patientId = 1L
        val doctorId = 1L
        val patient = Patient("Test Patient", "123", "ACTIVE", mutableListOf(), emptyList(), patientId)
        val doctor = AppUser("test@test.com", "pass", "John", "Doe", "123", mutableSetOf(Specialty("TO")), emptySet(), doctorId)

        `when`(patientService.getPatientById(patientId)).thenReturn(patient)
        `when`(appUserService.getAppUserById(doctorId)).thenReturn(doctor)
        `when`(patientService.addDoctorToPatient(patientId, doctorId)).thenReturn(false)

        // When/Then
        mockMvc.perform(
            post("/patients/$patientId/doctors/$doctorId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isInternalServerError)
    }
} 