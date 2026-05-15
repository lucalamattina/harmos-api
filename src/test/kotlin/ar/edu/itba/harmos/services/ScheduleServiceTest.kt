package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateScheduleRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.models.PatientStatus
import ar.edu.itba.harmos.models.Schedule
import ar.edu.itba.harmos.persistence.ScheduleRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.DayOfWeek
import java.util.*

// ⚠️ Potential bug (1): createSchedule does not validate that hourFrom is within 0–23 or that
//    the resulting hourTo stays within 0–23 at the service level. Bean Validation annotations on
//    CreateScheduleRequest (@Min/@Max) are only enforced by the Spring MVC layer; a caller that
//    bypasses the controller (e.g. another service, a test, or a script) could produce a Schedule
//    with hourTo > 23 or negative values with no exception thrown.
//
// ⚠️ Potential bug (2): When durationMinutes is null and hourTo is non-null but minuteTo IS null,
//    the service skips the first throw (hourTo is present) and hits the second throw for minuteTo.
//    Both branches emit the same generic message "Se requiere durationMinutes o hourTo/minuteTo",
//    which makes it impossible for callers to distinguish "hourTo missing" from "minuteTo missing".

@ExtendWith(MockitoExtension::class)
class ScheduleServiceTest {

    @Mock
    private lateinit var scheduleRepository: ScheduleRepository

    private lateinit var service: ScheduleService

    @BeforeEach
    fun setUp() {
        service = ScheduleService(scheduleRepository)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun createDoctor(id: Long = 1L) = AppUser(
        email = "doc@test.com",
        password = "x",
        firstName = "Doc",
        lastName = "Tor",
        phone = "0",
        roles = mutableSetOf(),
        id = id
    )

    private fun createPatient(id: Long = 1L) = Patient(
        firstName = "P",
        lastName = "Q",
        phone = "0",
        status = PatientStatus.ACTIVE,
        doctors = mutableListOf(),
        reports = emptyList(),
        id = id
    )

    private fun buildRequest(
        dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
        hourFrom: Int = 9,
        minuteFrom: Int = 0,
        hourTo: Int? = null,
        minuteTo: Int? = null,
        durationMinutes: Int? = null,
        doctorUserId: Long = 1L,
        patientId: Long = 1L
    ) = CreateScheduleRequest(
        dayOfWeek = dayOfWeek,
        hourFrom = hourFrom,
        minuteFrom = minuteFrom,
        hourTo = hourTo,
        minuteTo = minuteTo,
        durationMinutes = durationMinutes,
        doctorUserId = doctorUserId,
        patientId = patientId
    )

    // ── Group 1: createSchedule with durationMinutes ──────────────────────

    @Test
    fun `createSchedule calculates hourTo and minuteTo from durationMinutes`() {
        // Given: hourFrom=9, minuteFrom=0, durationMinutes=90
        // totalMinutesTo = 9*60 + 0 + 90 = 630 → hourTo=10, minuteTo=30
        val doctor = createDoctor()
        val patient = createPatient()
        val request = buildRequest(hourFrom = 9, minuteFrom = 0, durationMinutes = 90)

        val expectedSchedule = Schedule(DayOfWeek.MONDAY, 9, 0, 10, 30, doctor, patient)
        `when`(scheduleRepository.save(org.mockito.ArgumentMatchers.any(Schedule::class.java)))
            .thenReturn(expectedSchedule)

        // When
        service.createSchedule(request, doctor, patient)

        // Then
        val captor = ArgumentCaptor.forClass(Schedule::class.java)
        verify(scheduleRepository).save(captor.capture())
        assertThat(captor.value.hourTo).isEqualTo(10)
        assertThat(captor.value.minuteTo).isEqualTo(30)
    }

    @Test
    fun `createSchedule handles durationMinutes that crosses hour boundary`() {
        // Given: hourFrom=9, minuteFrom=45, durationMinutes=30
        // totalMinutesTo = 9*60 + 45 + 30 = 615 → hourTo=10, minuteTo=15
        val doctor = createDoctor()
        val patient = createPatient()
        val request = buildRequest(hourFrom = 9, minuteFrom = 45, durationMinutes = 30)

        val expectedSchedule = Schedule(DayOfWeek.MONDAY, 9, 45, 10, 15, doctor, patient)
        `when`(scheduleRepository.save(org.mockito.ArgumentMatchers.any(Schedule::class.java)))
            .thenReturn(expectedSchedule)

        // When
        service.createSchedule(request, doctor, patient)

        // Then
        val captor = ArgumentCaptor.forClass(Schedule::class.java)
        verify(scheduleRepository).save(captor.capture())
        assertThat(captor.value.hourTo).isEqualTo(10)
        assertThat(captor.value.minuteTo).isEqualTo(15)
    }

    // ── Group 2: createSchedule with explicit hourTo/minuteTo ─────────────

    @Test
    fun `createSchedule uses explicit hourTo and minuteTo when durationMinutes is null`() {
        // Given: hourFrom=9, minuteFrom=0, hourTo=10, minuteTo=30, durationMinutes=null
        // totalMinutesTo = 10*60 + 30 = 630 → hourTo=10, minuteTo=30
        val doctor = createDoctor()
        val patient = createPatient()
        val request = buildRequest(
            hourFrom = 9,
            minuteFrom = 0,
            hourTo = 10,
            minuteTo = 30,
            durationMinutes = null
        )

        val expectedSchedule = Schedule(DayOfWeek.MONDAY, 9, 0, 10, 30, doctor, patient)
        `when`(scheduleRepository.save(org.mockito.ArgumentMatchers.any(Schedule::class.java)))
            .thenReturn(expectedSchedule)

        // When
        service.createSchedule(request, doctor, patient)

        // Then
        val captor = ArgumentCaptor.forClass(Schedule::class.java)
        verify(scheduleRepository).save(captor.capture())
        assertThat(captor.value.hourTo).isEqualTo(10)
        assertThat(captor.value.minuteTo).isEqualTo(30)
    }

    // ── Group 3: validation errors ────────────────────────────────────────

    @Test
    fun `createSchedule throws IllegalArgumentException when durationMinutes is null and hourTo is null`() {
        // Given: neither durationMinutes nor hourTo are provided
        val request = buildRequest(durationMinutes = null, hourTo = null, minuteTo = null)

        // When / Then
        assertThrows<IllegalArgumentException> {
            service.createSchedule(request, createDoctor(), createPatient())
        }
    }

    @Test
    fun `createSchedule throws IllegalArgumentException when durationMinutes is null and minuteTo is null`() {
        // Given: hourTo is provided but minuteTo is missing — second null-check fires
        val request = buildRequest(durationMinutes = null, hourTo = 10, minuteTo = null)

        // When / Then
        assertThrows<IllegalArgumentException> {
            service.createSchedule(request, createDoctor(), createPatient())
        }
    }

    // ── Group 4: deleteScheduleById ───────────────────────────────────────

    @Test
    fun `deleteScheduleById returns true when schedule exists`() {
        // Given
        val doctor = createDoctor()
        val patient = createPatient()
        val schedule = Schedule(DayOfWeek.MONDAY, 9, 0, 10, 0, doctor, patient, id = 42L)
        `when`(scheduleRepository.findById(42L)).thenReturn(Optional.of(schedule))

        // When
        val result = service.deleteScheduleById(42L)

        // Then
        assertThat(result).isTrue
        verify(scheduleRepository).delete(schedule)
    }

    @Test
    fun `deleteScheduleById returns false when schedule not found`() {
        // Given
        `when`(scheduleRepository.findById(99L)).thenReturn(Optional.empty())

        // When
        val result = service.deleteScheduleById(99L)

        // Then
        assertThat(result).isFalse
    }
}
