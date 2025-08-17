package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.Patient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface PatientRepository : JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient>