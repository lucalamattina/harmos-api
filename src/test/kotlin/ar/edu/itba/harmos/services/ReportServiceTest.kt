package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.AppUserRole
import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.models.PatientStatus
import ar.edu.itba.harmos.models.Report
import ar.edu.itba.harmos.models.Role
import ar.edu.itba.harmos.persistence.ReportRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

@ExtendWith(MockitoExtension::class)
class ReportServiceTest {

    @Mock private lateinit var reportRepository: ReportRepository
    @Mock private lateinit var patientService: PatientService
    @Mock private lateinit var specialtyService: SpecialtyService
    @Mock private lateinit var cloudinaryService: CloudinaryService
    @Mock private lateinit var notificationService: NotificationService
    @Mock private lateinit var asyncNotificationService: AsyncNotificationService

    private lateinit var service: ReportService

    // Kotlin-safe Mockito any() helper: avoids non-null cast NPE by using type erasure.
    @Suppress("UNCHECKED_CAST")
    private fun <T> anySpec(): Specification<T>? = any(Specification::class.java) as? Specification<T>

    @BeforeEach
    fun setUp() {
        service = ReportService(
            reportRepository,
            patientService,
            specialtyService,
            cloudinaryService,
            notificationService,
            asyncNotificationService
        )
    }

    private fun stubEmptyPage() {
        `when`(reportRepository.findAll(anySpec<Report>(), any(Pageable::class.java)))
            .thenReturn(PageImpl(emptyList()))
    }

    private fun capturePageable(): ArgumentCaptor<Pageable> {
        val captor = ArgumentCaptor.forClass(Pageable::class.java)
        verify(reportRepository).findAll(anySpec<Report>(), captor.capture())
        return captor
    }

    private fun userWithRole(roleName: String): AppUser {
        val role = Role(role = roleName, id = 1L)
        return AppUser(
            email = "u@test.com", password = "x", firstName = "A", lastName = "B", phone = "0",
            roles = mutableSetOf(role), id = 1L
        )
    }

    // ========================= isAdmin =========================

    @Test
    fun `isAdmin returns true when user has ADMINISTRATOR role`() {
        val admin = userWithRole(AppUserRole.ADMINISTRATOR.roleName)
        assertThat(service.isAdmin(admin)).isTrue
    }

    @Test
    fun `isAdmin returns false when user has only DOCTOR role`() {
        val doctor = userWithRole(AppUserRole.DOCTOR.roleName)
        assertThat(service.isAdmin(doctor)).isFalse
    }

    @Test
    fun `isAdmin returns false when user has no roles`() {
        val user = AppUser(email = "u@test.com", password = "x", firstName = "A", lastName = "B", phone = "0", roles = mutableSetOf(), id = 1L)
        assertThat(service.isAdmin(user)).isFalse
    }

    // ========================= sort-field whitelist =========================

    @Test
    fun `getAllReportsPaginated defaults sortBy to date when field is not whitelisted`() {
        // Given
        stubEmptyPage()

        // When
        service.getAllReportsPaginated(sortBy = "malicious_field", sortDirection = "desc")

        // Then
        val sort = capturePageable().value.sort
        assertThat(sort.getOrderFor("date")).isNotNull
        assertThat(sort.getOrderFor("malicious_field")).isNull()
    }

    @Test
    fun `getAllReportsPaginated accepts whitelisted sortBy field id`() {
        stubEmptyPage()

        service.getAllReportsPaginated(sortBy = "id", sortDirection = "asc")

        assertThat(capturePageable().value.sort.getOrderFor("id")).isNotNull
    }

    @Test
    fun `getAllReportsPaginated accepts whitelisted sortBy field title`() {
        stubEmptyPage()

        service.getAllReportsPaginated(sortBy = "title", sortDirection = "asc")

        assertThat(capturePageable().value.sort.getOrderFor("title")).isNotNull
    }

    // ========================= sort-direction whitelist =========================

    @Test
    fun `getAllReportsPaginated defaults sortDirection to DESC when value is invalid`() {
        stubEmptyPage()

        service.getAllReportsPaginated(sortBy = "date", sortDirection = "invalid_dir")

        val order = capturePageable().value.sort.getOrderFor("date")
        assertThat(order?.direction).isEqualTo(Sort.Direction.DESC)
    }

    @Test
    fun `getAllReportsPaginated uses ASC direction when asc is specified`() {
        stubEmptyPage()

        service.getAllReportsPaginated(sortBy = "date", sortDirection = "asc")

        val order = capturePageable().value.sort.getOrderFor("date")
        assertThat(order?.direction).isEqualTo(Sort.Direction.ASC)
    }

    @Test
    fun `getAllReportsPaginated is case-insensitive for sortDirection`() {
        stubEmptyPage()

        service.getAllReportsPaginated(sortBy = "date", sortDirection = "DESC")

        val order = capturePageable().value.sort.getOrderFor("date")
        assertThat(order?.direction).isEqualTo(Sort.Direction.DESC)
    }

    // ========================= isDoctorAssignedToPatient =========================

    private fun doctorWithId(id: Long) = AppUser(
        email = "doc$id@test.com", password = "x", firstName = "Doc", lastName = "$id", phone = "0",
        roles = mutableSetOf(Role(role = AppUserRole.DOCTOR.roleName, id = 2L)),
        id = id
    )

    @Test
    fun `isDoctorAssignedToPatient is true when the doctor is in the patient's doctors`() {
        val doctor = doctorWithId(5L)
        val patient = Patient("P", "L", "555", PatientStatus.ACTIVE, mutableListOf(doctorWithId(5L)), emptyList(), 7L)
        `when`(patientService.getPatientById(7L)).thenReturn(patient)

        assertThat(service.isDoctorAssignedToPatient(doctor, 7L)).isTrue()
    }

    @Test
    fun `isDoctorAssignedToPatient is false when the doctor is not assigned`() {
        val patient = Patient("P", "L", "555", PatientStatus.ACTIVE, mutableListOf(doctorWithId(6L)), emptyList(), 7L)
        `when`(patientService.getPatientById(7L)).thenReturn(patient)

        assertThat(service.isDoctorAssignedToPatient(doctorWithId(5L), 7L)).isFalse()
    }

    @Test
    fun `isDoctorAssignedToPatient is false when the patient does not exist`() {
        `when`(patientService.getPatientById(7L)).thenReturn(null)

        assertThat(service.isDoctorAssignedToPatient(doctorWithId(5L), 7L)).isFalse()
    }
}
