package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.EditPatientRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.models.PatientStatus
import ar.edu.itba.harmos.persistence.AppUserRepository
import ar.edu.itba.harmos.persistence.PatientRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class PatientServiceTest {

    @Mock private lateinit var patientRepository: PatientRepository
    @Mock private lateinit var appUserRepository: AppUserRepository

    private lateinit var service: PatientService

    @BeforeEach
    fun setUp() {
        service = PatientService(patientRepository, appUserRepository)
    }

    private fun createPatient(id: Long = 1L, status: PatientStatus = PatientStatus.ACTIVE) =
        Patient(
            firstName = "Jane",
            lastName = "Doe",
            phone = "555",
            status = status,
            doctors = mutableListOf(),
            reports = emptyList(),
            id = id
        )

    private fun createDoctor(id: Long = 10L) = AppUser(
        email = "doc@test.com", password = "x", firstName = "Doc", lastName = "Tor", phone = "0",
        roles = mutableSetOf(), id = id
    )

    // ========================= updatePatient =========================

    @Test
    fun `updatePatient throws RuntimeException when patient does not exist`() {
        // Given
        `when`(patientRepository.findById(99L)).thenReturn(Optional.empty())

        // When / Then
        assertThrows<RuntimeException> {
            service.updatePatient(99L, EditPatientRequest(firstName = "New", lastName = null, phone = null, status = null, doctorIds = null))
        }
    }

    @Test
    fun `updatePatient updates firstName when provided`() {
        // Given
        val patient = createPatient()
        `when`(patientRepository.findById(1L)).thenReturn(Optional.of(patient))
        `when`(patientRepository.save(patient)).thenReturn(patient)

        // When
        val result = service.updatePatient(1L, EditPatientRequest("Updated", null, null, null, null))

        // Then
        assertThat(result.firstName).isEqualTo("Updated")
        verify(patientRepository).save(patient)
    }

    @Test
    fun `updatePatient updates status when provided`() {
        // Given
        val patient = createPatient(status = PatientStatus.ACTIVE)
        `when`(patientRepository.findById(1L)).thenReturn(Optional.of(patient))
        `when`(patientRepository.save(patient)).thenReturn(patient)

        // When
        val result = service.updatePatient(1L, EditPatientRequest(null, null, null, PatientStatus.INACTIVE, null))

        // Then
        assertThat(result.status).isEqualTo(PatientStatus.INACTIVE)
    }

    @Test
    fun `updatePatient replaces doctors list when doctorIds provided`() {
        // Given
        val patient = createPatient()
        val doctor = createDoctor()
        `when`(patientRepository.findById(1L)).thenReturn(Optional.of(patient))
        `when`(appUserRepository.findAllById(listOf(10L))).thenReturn(listOf(doctor))
        `when`(patientRepository.save(patient)).thenReturn(patient)

        // When
        val result = service.updatePatient(1L, EditPatientRequest(null, null, null, null, listOf(10L)))

        // Then
        assertThat(result.doctors).containsExactly(doctor)
    }

    @Test
    fun `updatePatient clears doctors list when doctorIds is empty list`() {
        // Given
        val doctor = createDoctor()
        val patient = createPatient().also { it.doctors.add(doctor) }
        `when`(patientRepository.findById(1L)).thenReturn(Optional.of(patient))
        `when`(appUserRepository.findAllById(emptyList())).thenReturn(emptyList())
        `when`(patientRepository.save(patient)).thenReturn(patient)

        // When
        val result = service.updatePatient(1L, EditPatientRequest(null, null, null, null, emptyList()))

        // Then
        assertThat(result.doctors).isEmpty()
    }

    @Test
    fun `updatePatient does not modify fields when edit request has all nulls except doctorIds`() {
        // Given
        val patient = createPatient()
        `when`(patientRepository.findById(1L)).thenReturn(Optional.of(patient))
        `when`(patientRepository.save(patient)).thenReturn(patient)

        // When — all fields null means nothing changes
        val result = service.updatePatient(1L, EditPatientRequest(null, null, null, null, null))

        // Then — original values preserved
        assertThat(result.firstName).isEqualTo("Jane")
        assertThat(result.lastName).isEqualTo("Doe")
    }

    // ========================= addDoctorToPatient =========================

    @Test
    fun `addDoctorToPatient returns false when patient does not exist`() {
        `when`(patientRepository.existsById(1L)).thenReturn(false)
        assertThat(service.addDoctorToPatient(1L, 10L)).isFalse
    }

    @Test
    fun `addDoctorToPatient returns false when doctor does not exist`() {
        `when`(patientRepository.existsById(1L)).thenReturn(true)
        `when`(appUserRepository.existsById(10L)).thenReturn(false)
        assertThat(service.addDoctorToPatient(1L, 10L)).isFalse
    }

    @Test
    fun `addDoctorToPatient returns true and calls repository when both exist`() {
        `when`(patientRepository.existsById(1L)).thenReturn(true)
        `when`(appUserRepository.existsById(10L)).thenReturn(true)

        val result = service.addDoctorToPatient(1L, 10L)

        assertThat(result).isTrue
        verify(patientRepository).addDoctorToPatient(1L, 10L)
    }

    // ========================= removeDoctorFromPatient =========================

    @Test
    fun `removeDoctorFromPatient returns false when patient does not exist`() {
        `when`(patientRepository.existsById(1L)).thenReturn(false)
        assertThat(service.removeDoctorFromPatient(1L, 10L)).isFalse
    }

    @Test
    fun `removeDoctorFromPatient returns true and calls repository when both exist`() {
        `when`(patientRepository.existsById(1L)).thenReturn(true)
        `when`(appUserRepository.existsById(10L)).thenReturn(true)

        val result = service.removeDoctorFromPatient(1L, 10L)

        assertThat(result).isTrue
        verify(patientRepository).removeDoctorFromPatient(1L, 10L)
    }
}
