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
    
    // Paginated methods - basic filters
    fun findAllByOrderByDateDesc(pageable: Pageable): Page<Report>
    fun findByPatientIdOrderByDateDesc(patientId: Long, pageable: Pageable): Page<Report>
    fun findByPatientIdAndSpecialtyIdOrderByDateDesc(patientId: Long, specialtyId: Long, pageable: Pageable): Page<Report>
    fun findByDoctorIdOrderByDateDesc(doctorId: Long, pageable: Pageable): Page<Report>
    fun findBySpecialtyIdOrderByDateDesc(specialtyId: Long, pageable: Pageable): Page<Report>
    
    // New paginated methods - title filters
    fun findByTitleContainingIgnoreCaseOrderByDateDesc(title: String, pageable: Pageable): Page<Report>
    
    // Combined filters with title
    fun findByTitleContainingIgnoreCaseAndPatientIdOrderByDateDesc(title: String, patientId: Long, pageable: Pageable): Page<Report>
    fun findByTitleContainingIgnoreCaseAndDoctorIdOrderByDateDesc(title: String, doctorId: Long, pageable: Pageable): Page<Report>
    fun findByTitleContainingIgnoreCaseAndSpecialtyIdOrderByDateDesc(title: String, specialtyId: Long, pageable: Pageable): Page<Report>
    fun findByTitleContainingIgnoreCaseAndPatientIdAndSpecialtyIdOrderByDateDesc(title: String, patientId: Long, specialtyId: Long, pageable: Pageable): Page<Report>
    fun findByTitleContainingIgnoreCaseAndPatientIdAndDoctorIdOrderByDateDesc(title: String, patientId: Long, doctorId: Long, pageable: Pageable): Page<Report>
    fun findByTitleContainingIgnoreCaseAndSpecialtyIdAndDoctorIdOrderByDateDesc(title: String, specialtyId: Long, doctorId: Long, pageable: Pageable): Page<Report>
    
    // Combined filters without title
    fun findByPatientIdAndDoctorIdOrderByDateDesc(patientId: Long, doctorId: Long, pageable: Pageable): Page<Report>
    fun findBySpecialtyIdAndDoctorIdOrderByDateDesc(specialtyId: Long, doctorId: Long, pageable: Pageable): Page<Report>
    fun findByPatientIdAndSpecialtyIdAndDoctorIdOrderByDateDesc(patientId: Long, specialtyId: Long, doctorId: Long, pageable: Pageable): Page<Report>
    
    // All filters combined
    fun findByTitleContainingIgnoreCaseAndPatientIdAndSpecialtyIdAndDoctorIdOrderByDateDesc(title: String, patientId: Long, specialtyId: Long, doctorId: Long, pageable: Pageable): Page<Report>
} 