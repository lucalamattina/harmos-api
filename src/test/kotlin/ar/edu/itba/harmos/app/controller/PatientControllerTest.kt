package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.models.PatientStatus
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.CloudinaryService
import ar.edu.itba.harmos.services.PatientService
import ar.edu.itba.harmos.services.ReportService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@ExtendWith(MockitoExtension::class)
class PatientControllerTest {

    @Mock private lateinit var patientService: PatientService
    @Mock private lateinit var appUserService: AppUserService
    @Mock private lateinit var reportService: ReportService
    @Mock private lateinit var cloudinaryService: CloudinaryService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val controller = PatientController(patientService, appUserService, reportService, cloudinaryService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    private fun patient(doctors: MutableList<AppUser> = mutableListOf()) =
        Patient("Test", "Patient", "123", PatientStatus.ACTIVE, doctors, emptyList(), 1L)

    private fun doctor() = AppUser(
        email = "test@test.com", password = "pass", firstName = "John", lastName = "Doe",
        phone = "123", specialty = null, roles = mutableSetOf()
    )

    @Test
    fun `addDoctorToPatient should return NO_CONTENT when doctor is successfully added`() {
        val doctor = doctor()
        val patient = patient()
        `when`(patientService.getPatientById(1L)).thenReturn(patient)
        `when`(appUserService.getAppUserById(1L)).thenReturn(doctor)
        `when`(patientService.addDoctorToPatient(1L, 1L)).thenReturn(true)

        mockMvc.perform(post("/patients/1/doctors/1").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `addDoctorToPatient should return NO_CONTENT when doctor is already assigned`() {
        val doctor = doctor()
        val patient = patient(mutableListOf(doctor))
        `when`(patientService.getPatientById(1L)).thenReturn(patient)
        `when`(appUserService.getAppUserById(1L)).thenReturn(doctor)

        mockMvc.perform(post("/patients/1/doctors/1").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `addDoctorToPatient should return NOT_FOUND when patient does not exist`() {
        `when`(patientService.getPatientById(1L)).thenReturn(null)

        mockMvc.perform(post("/patients/1/doctors/1").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `addDoctorToPatient should return NOT_FOUND when doctor does not exist`() {
        `when`(patientService.getPatientById(1L)).thenReturn(patient())
        `when`(appUserService.getAppUserById(1L)).thenReturn(null)

        mockMvc.perform(post("/patients/1/doctors/1").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `addDoctorToPatient should return INTERNAL_SERVER_ERROR when service fails`() {
        val doctor = doctor()
        `when`(patientService.getPatientById(1L)).thenReturn(patient())
        `when`(appUserService.getAppUserById(1L)).thenReturn(doctor)
        `when`(patientService.addDoctorToPatient(1L, 1L)).thenReturn(false)

        mockMvc.perform(post("/patients/1/doctors/1").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError)
    }
}
