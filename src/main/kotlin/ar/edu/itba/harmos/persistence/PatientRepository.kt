package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.Patient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PatientRepository : JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    @Modifying
    @Query(
        value = "INSERT INTO patient_doctor (patient_id, doctor_id) VALUES (:patientId, :doctorId) ON CONFLICT DO NOTHING",
        nativeQuery = true
    )
    fun addDoctorToPatient(@Param("patientId") patientId: Long, @Param("doctorId") doctorId: Long)

    @Modifying
    @Query(
        value = "DELETE FROM patient_doctor WHERE patient_id = :patientId AND doctor_id = :doctorId",
        nativeQuery = true
    )
    fun removeDoctorFromPatient(@Param("patientId") patientId: Long, @Param("doctorId") doctorId: Long)
}