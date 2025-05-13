package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreatePatientRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.persistence.AppUserRepository
import ar.edu.itba.harmos.persistence.PatientRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@Service
class PatientService( private val patientRepository: PatientRepository,
                      private val appUserRepository: AppUserRepository) {

    @Transactional
    fun createPatient(createPatientRequest: CreatePatientRequest): Patient? {
        if (patientRepository.findByName(createPatientRequest.name) != null) {
            return null
        }

        // Obtener los usuarios por sus IDs
        val users = createPatientRequest.userIds.mapNotNull { userId ->
            appUserRepository.findById(userId).orElse(null)
        }.toMutableList()

        val patient = Patient(
            createPatientRequest.name,
            createPatientRequest.phone,
            createPatientRequest.status,
            users,
            emptyList()
        )
        return patientRepository.save(patient)
    }

    fun getPatientsContainingName(name: String, pageable: Pageable): Page<Patient> {
        val sortedPageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.ASC, "name"))
        return patientRepository.findByNameContainingIgnoreCase(name, sortedPageable)
    }

    fun getPatientByName(name: String): Patient? {
        return patientRepository.findByName(name)
    }

    fun getPatientById(id: Long): Patient? {
        val opt = patientRepository.findById(id)
        if (opt.isPresent) {
            return opt.get()
        }
        return null
    }

    fun deletePatientById(id: Long): Boolean {
        val patient = patientRepository.findById(id)
        return if (patient.isPresent) {
            patientRepository.delete(patient.get())
            true
        } else {
            false
        }
    }

    fun getPatients(pageable: Pageable): Page<Patient> {
        val sortedPageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.ASC, "name"))
        return patientRepository.findAll(sortedPageable)
    }

    @Transactional
    fun addUserToPatient(patientId: Long, userId: Long): Boolean {
        val patientOpt = patientRepository.findById(patientId)
        val userOpt = appUserRepository.findById(userId)

        if (patientOpt.isPresent && userOpt.isPresent) {
            val patient = patientOpt.get()
            val user = userOpt.get()

            // Check if user is already assigned to the patient
            if (patient.doctors.contains(user)) {
                return false
            }

            patient.doctors.add(user)
            patientRepository.save(patient)
            return true
        }
        return false
    }
}