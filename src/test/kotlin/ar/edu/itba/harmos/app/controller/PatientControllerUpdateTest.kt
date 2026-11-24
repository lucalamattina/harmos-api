package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.EditPatientRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.AppUserRole
import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.models.PatientStatus
import ar.edu.itba.harmos.models.Role
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.CloudinaryService
import ar.edu.itba.harmos.services.PatientService
import ar.edu.itba.harmos.services.ReportService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus

@ExtendWith(MockitoExtension::class)
class PatientControllerUpdateTest {

    @Mock private lateinit var patientService: PatientService
    @Mock private lateinit var appUserService: AppUserService
    @Mock private lateinit var reportService: ReportService
    @Mock private lateinit var cloudinaryService: CloudinaryService

    private lateinit var controller: PatientController

    @BeforeEach
    fun setUp() {
        controller = PatientController(patientService, appUserService, reportService, cloudinaryService)
    }

    private fun adminUser() = AppUser(
        email = "admin@test.com", password = "x", firstName = "Admin", lastName = "User", phone = "0",
        roles = mutableSetOf(Role(role = AppUserRole.ADMINISTRATOR.roleName, id = 1L)),
        id = 1L
    )

    private fun doctorUser() = AppUser(
        email = "doc@test.com", password = "x", firstName = "Doc", lastName = "Tor", phone = "0",
        roles = mutableSetOf(Role(role = AppUserRole.DOCTOR.roleName, id = 2L)),
        id = 2L
    )

    private fun editRequest() = EditPatientRequest("Updated", null, null, null, null)

    @Test
    fun `update returns 401 when no user is authenticated`() {
        val response = controller.updatePatient(1L, editRequest(), null)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `update returns 403 FORBIDDEN when user is not an administrator`() {
        val response = controller.updatePatient(1L, editRequest(), doctorUser())
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        @Suppress("UNCHECKED_CAST")
        assertThat((response.body as Map<String, Any>)["error"])
            .isEqualTo("Forbidden: only administrators can edit patients")
        verifyNoInteractions(patientService)
    }

    @Test
    fun `update returns 200 OK when user is an administrator`() {
        val request = editRequest()
        val updated = Patient("Updated", "Doe", "555", PatientStatus.ACTIVE, mutableListOf(), emptyList(), 1L)
        `when`(patientService.updatePatient(1L, request)).thenReturn(updated)

        val response = controller.updatePatient(1L, request, adminUser())
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `update returns 404 NOT_FOUND when admin edits a missing patient`() {
        val request = editRequest()
        `when`(patientService.updatePatient(1L, request)).thenThrow(RuntimeException("not found"))

        val response = controller.updatePatient(1L, request, adminUser())
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
}
