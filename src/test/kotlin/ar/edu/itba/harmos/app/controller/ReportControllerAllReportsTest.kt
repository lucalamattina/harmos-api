package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.AppUserRole
import ar.edu.itba.harmos.models.Report
import ar.edu.itba.harmos.models.Role
import ar.edu.itba.harmos.services.CloudinaryService
import ar.edu.itba.harmos.services.ReportService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.http.HttpStatus

/** Access rules for GET /reports/all: admins see everything, doctors only their assigned patients. */
@ExtendWith(MockitoExtension::class)
class ReportControllerAllReportsTest {

    @Mock private lateinit var reportService: ReportService
    @Mock private lateinit var cloudinaryService: CloudinaryService

    private lateinit var controller: ReportController

    @BeforeEach
    fun setUp() {
        controller = ReportController(reportService, cloudinaryService)
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

    private fun emptyPage(): Page<Report> = PageImpl(emptyList())

    private fun callAll(user: AppUser?, patientId: Long? = null) =
        controller.getAllReportsForAdmin(patientId, null, null, null, 0, 10, "date", "desc", user)

    @Test
    fun `returns 401 when no user is authenticated`() {
        assertThat(callAll(null).statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `doctor gets 403 when filtering by a patient they are not assigned to`() {
        val doctor = doctorUser()
        `when`(reportService.isAdmin(doctor)).thenReturn(false)
        `when`(reportService.isDoctorAssignedToPatient(doctor, 7L)).thenReturn(false)

        val response = callAll(doctor, patientId = 7L)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        @Suppress("UNCHECKED_CAST")
        assertThat((response.body as Map<String, Any>)["error"]).isEqualTo("No tienes acceso a este paciente")
        verify(reportService).isAdmin(doctor)
        verify(reportService).isDoctorAssignedToPatient(doctor, 7L)
        verifyNoMoreInteractions(reportService) // no report query was run
    }

    @Test
    fun `doctor filtering by an assigned patient gets the doctor-scoped query`() {
        val doctor = doctorUser()
        `when`(reportService.isAdmin(doctor)).thenReturn(false)
        `when`(reportService.isDoctorAssignedToPatient(doctor, 7L)).thenReturn(true)
        `when`(reportService.getReportsForDoctorPaginated(doctor, 7L, null, null, null, 0, 10, "date", "desc"))
            .thenReturn(emptyPage())

        assertThat(callAll(doctor, patientId = 7L).statusCode).isEqualTo(HttpStatus.OK)
        verify(reportService).getReportsForDoctorPaginated(doctor, 7L, null, null, null, 0, 10, "date", "desc")
    }

    @Test
    fun `doctor without a patient filter gets the doctor-scoped query, not every report`() {
        val doctor = doctorUser()
        `when`(reportService.isAdmin(doctor)).thenReturn(false)
        `when`(reportService.getReportsForDoctorPaginated(doctor, null, null, null, null, 0, 10, "date", "desc"))
            .thenReturn(emptyPage())

        assertThat(callAll(doctor).statusCode).isEqualTo(HttpStatus.OK)
        verify(reportService).isAdmin(doctor)
        verify(reportService).getReportsForDoctorPaginated(doctor, null, null, null, null, 0, 10, "date", "desc")
        verifyNoMoreInteractions(reportService)
    }

    @Test
    fun `admin gets every report for any patient without an assignment check`() {
        val admin = adminUser()
        `when`(reportService.isAdmin(admin)).thenReturn(true)
        `when`(reportService.getAllReportsPaginated(7L, null, null, null, 0, 10, "date", "desc"))
            .thenReturn(emptyPage())

        assertThat(callAll(admin, patientId = 7L).statusCode).isEqualTo(HttpStatus.OK)
        verify(reportService).isAdmin(admin)
        verify(reportService).getAllReportsPaginated(7L, null, null, null, 0, 10, "date", "desc")
        verifyNoMoreInteractions(reportService)
    }
}
