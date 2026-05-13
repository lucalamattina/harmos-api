package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreatePatientRequest
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
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus

@ExtendWith(MockitoExtension::class)
class PatientControllerCreateTest {

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

    private fun createRequest() = CreatePatientRequest(
        firstName = "Jane", lastName = "Doe", phone = "555", status = PatientStatus.ACTIVE
    )

    @Test
    fun `create returns 401 when no user is authenticated`() {
        val response = controller.create(createRequest(), null)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `create returns 403 FORBIDDEN when user is not an administrator`() {
        val response = controller.create(createRequest(), doctorUser())
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        @Suppress("UNCHECKED_CAST")
        assertThat((response.body as Map<String, Any>)["error"])
            .isEqualTo("Forbidden: only administrators can create patients")
    }

    @Test
    fun `create returns 201 CREATED when user is an administrator`() {
        val request = createRequest()
        val saved = Patient("Jane", "Doe", "555", PatientStatus.ACTIVE, mutableListOf(), emptyList(), 10L)
        `when`(patientService.createPatient(request)).thenReturn(saved)

        val response = controller.create(request, adminUser())
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }
}
