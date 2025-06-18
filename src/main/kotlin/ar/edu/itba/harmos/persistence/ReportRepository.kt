package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.Report
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
 
@Repository
interface ReportRepository : CrudRepository<Report, Long> {
    fun findByPatientId(patientId: Long): List<Report>
    fun findByDoctorId(doctorId: Long): List<Report>
    fun findBySpecialtyId(specialtyId: Long): List<Report>
    fun findByPatientIdAndSpecialtyId(patientId: Long, specialtyId: Long): List<Report>
    fun findByPatientIdOrderByDateDesc(patientId: Long): List<Report>
    fun findByPatientIdAndSpecialtyIdOrderByDateDesc(patientId: Long, specialtyId: Long): List<Report>
    fun findByDoctorIdOrderByDateDesc(doctorId: Long): List<Report>
} 