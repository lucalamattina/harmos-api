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

@Service
class PatientService( private val patientRepository: PatientRepository,
                      private val appUserRepository: AppUserRepository) {

    fun createPatient(createPatientRequest: CreatePatientRequest): Patient? {
        if (patientRepository.findByName(createPatientRequest.name) != null) {
            return null
        }
        val patient = Patient(
            createPatientRequest.name,
            createPatientRequest.phone,
            createPatientRequest.status,
            mutableListOf(),
            emptyList()
        )
        return patientRepository.save(patient)
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
        return patientRepository.findAll(pageable)
    }

    @Transactional
    fun addDoctorToPatient(patientId: Long, doctorId: Long): Boolean {
        val patientOpt = patientRepository.findById(patientId)
        val doctorOpt = appUserRepository.findById(doctorId)

        if (patientOpt.isPresent && doctorOpt.isPresent) {
            val patient = patientOpt.get()
            val doctor = doctorOpt.get()

            // Check if doctor is already assigned to the patient
            if (patient.doctors.contains(doctor)) {
                return false
            }

            patient.doctors.add(doctor)
            patientRepository.save(patient)
            return true
        }
        return false
    }
}