package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.Report
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
 
@Repository
interface ReportRepository : PagingAndSortingRepository<Report, Long> {
    // Non-paginated methods (for backward compatibility)
    fun findByPatientId(patientId: Long): List<Report>
    fun findByDoctorId(doctorId: Long): List<Report>
    fun findBySpecialtyId(specialtyId: Long): List<Report>
    fun findByPatientIdAndSpecialtyId(patientId: Long, specialtyId: Long): List<Report>
    fun findByPatientIdOrderByDateDesc(patientId: Long): List<Report>
    fun findByPatientIdAndSpecialtyIdOrderByDateDesc(patientId: Long, specialtyId: Long): List<Report>
    fun findByDoctorIdOrderByDateDesc(doctorId: Long): List<Report>
    
    // Paginated methods
    fun findAllByOrderByDateDesc(pageable: Pageable): Page<Report>
    fun findByPatientIdOrderByDateDesc(patientId: Long, pageable: Pageable): Page<Report>
    fun findByPatientIdAndSpecialtyIdOrderByDateDesc(patientId: Long, specialtyId: Long, pageable: Pageable): Page<Report>
    fun findByDoctorIdOrderByDateDesc(doctorId: Long, pageable: Pageable): Page<Report>
    fun findBySpecialtyIdOrderByDateDesc(specialtyId: Long, pageable: Pageable): Page<Report>
} 