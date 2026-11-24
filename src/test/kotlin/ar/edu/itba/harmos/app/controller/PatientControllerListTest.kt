package ar.edu.itba.harmos.app.controller

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
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus

@ExtendWith(MockitoExtension::class)
class PatientControllerListTest {

    @Mock private lateinit var patientService: PatientService
    @Mock private lateinit var appUserService: AppUserService
    @Mock private lateinit var reportService: ReportService
    @Mock private lateinit var cloudinaryService: CloudinaryService

    private lateinit var controller: PatientController
    private val pageable = PageRequest.of(0, 10)

    @BeforeEach
    fun setUp() {
        controller = PatientController(patientService, appUserService, reportService, cloudinaryService)
    }

    private fun doctorUser() = AppUser(
        email = "doc@test.com", password = "x", firstName = "Doc", lastName = "Tor", phone = "0",
        roles = mutableSetOf(Role(role = AppUserRole.DOCTOR.roleName, id = 2L)),
        id = 2L
    )

    private fun page(vararg ids: Long): Page<Patient> = PageImpl(
        ids.map { Patient("P$it", "Last", "555", PatientStatus.ACTIVE, mutableListOf(), emptyList(), it) }
    )

    @Test
    fun `getPatients returns 401 when no user is authenticated`() {
        val response = controller.getPatients(null, null, null, null, null, pageable, null)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        verifyNoInteractions(patientService)
    }

    @Test
    fun `getPatients lets a doctor list all patients, not only their own`() {
        // doctorId is passed as null, so the service is not filtering by the doctor's id
        `when`(patientService.getPatients(null, null, null, null, null, pageable)).thenReturn(page(1L, 2L, 3L))

        val response = controller.getPatients(null, null, null, null, null, pageable, doctorUser())
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        assertThat((response.body as Page<Any>).totalElements).isEqualTo(3)
    }

    @Test
    fun `getPatients honors the doctorId filter sent by a doctor`() {
        `when`(patientService.getPatients(null, null, null, null, 99L, pageable)).thenReturn(page(5L))

        val response = controller.getPatients(null, null, null, null, 99L, pageable, doctorUser())
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        assertThat((response.body as Page<Any>).totalElements).isEqualTo(1)
    }
}
