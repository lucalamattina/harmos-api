package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.models.PatientStatus
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

@Repository
interface PatientRepository : CrudRepository<Patient, Long> {
    fun findByName(name: String): Patient?

    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Patient>

    fun findAll(pageable: Pageable): Page<Patient>

    fun findAllByOrderByNameAsc(pageable: Pageable): Page<Patient>

    fun findByDoctorsContainingAndNameContainingIgnoreCase(doctor: AppUser, name: String, pageable: Pageable): Page<Patient>

    fun findByStatus(status: PatientStatus, pageable: Pageable): Page<Patient>

    @Query("""
        SELECT DISTINCT p FROM Patient p 
        INNER JOIN p.doctors d 
        WHERE LOWER(d.firstName) LIKE LOWER(CONCAT('%', :doctorName, '%')) 
        OR LOWER(d.lastName) LIKE LOWER(CONCAT('%', :doctorName, '%'))
    """)
    fun findByDoctorNameContainingIgnoreCase(@Param("doctorName") doctorName: String, pageable: Pageable): Page<Patient>
}