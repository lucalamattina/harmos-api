package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Patient
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Repository
interface PatientRepository : CrudRepository<Patient, Long> {
    fun findByName(name: String): Patient?

    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Patient>

    fun findAll(pageable: Pageable): Page<Patient>

    fun findAllByOrderByNameAsc(pageable: Pageable): Page<Patient>

    fun findByDoctorsContainingAndNameContainingIgnoreCase(doctor: AppUser, name: String, pageable: Pageable): Page<Patient>
}