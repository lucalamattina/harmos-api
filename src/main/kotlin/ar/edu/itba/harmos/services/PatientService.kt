package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreatePatientRequest
import ar.edu.itba.harmos.dtos.requests.EditPatientRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.models.PatientStatus
import ar.edu.itba.harmos.persistence.AppUserRepository
import ar.edu.itba.harmos.persistence.PatientRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageImpl

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

    fun getPatientsContainingName(name: String, pageable: Pageable, status: PatientStatus? = null): Page<Patient> {
        return if (status != null) {
            patientRepository.findByNameContainingIgnoreCaseAndStatus(name, status, pageable)
        } else {
            patientRepository.findByNameContainingIgnoreCase(name, pageable)
        }
    }

    fun getPatientsByDoctorName(doctorName: String, pageable: Pageable, status: PatientStatus? = null): Page<Patient> {
        return if (status != null) {
            patientRepository.findByDoctorNameContainingIgnoreCaseAndStatus(doctorName, status, pageable)
        } else {
            patientRepository.findByDoctorNameContainingIgnoreCase(doctorName, pageable)
        }
    }

    fun getPatientsByDoctorAndName(doctorName: String, patientName: String, pageable: Pageable, status: PatientStatus? = null): Page<Patient> {
        return if (status != null) {
            patientRepository.findByDoctorNameContainingIgnoreCaseAndPatientNameContainingIgnoreCaseAndStatus(
                doctorName, patientName, status, pageable
            )
        } else {
            patientRepository.findByDoctorNameContainingIgnoreCaseAndPatientNameContainingIgnoreCase(
                doctorName, patientName, pageable
            )
        }
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

    fun getPatients(pageable: Pageable, status: PatientStatus? = null): Page<Patient> {
        return if (status != null) {
            patientRepository.findByStatus(status, pageable)
        } else {
            patientRepository.findAll(pageable)
        }
    }

    fun getPatientsByDoctorSpecialty(specialty: String, pageable: Pageable, status: PatientStatus? = null): Page<Patient> {
        return if (status != null) {
            patientRepository.findByDoctorSpecialtyAndStatus(specialty, status, pageable)
        } else {
            patientRepository.findByDoctorSpecialty(specialty, pageable)
        }
    }

    fun getPatientsByDoctorSpecialtyAndName(specialty: String, patientName: String, pageable: Pageable, status: PatientStatus? = null): Page<Patient> {
        return if (status != null) {
            patientRepository.findByDoctorSpecialtyAndPatientNameContainingIgnoreCaseAndStatus(specialty, patientName, status, pageable)
        } else {
            patientRepository.findByDoctorSpecialtyAndPatientNameContainingIgnoreCase(specialty, patientName, pageable)
        }
    }

    fun getPatientsByDoctorSpecialtyAndDoctorName(specialty: String, doctorName: String, pageable: Pageable, status: PatientStatus? = null): Page<Patient> {
        return if (status != null) {
            patientRepository.findByDoctorSpecialtyAndDoctorNameContainingIgnoreCaseAndStatus(specialty, doctorName, status, pageable)
        } else {
            patientRepository.findByDoctorSpecialtyAndDoctorNameContainingIgnoreCase(specialty, doctorName, pageable)
        }
    }

    fun getPatientsByDoctorSpecialtyAndDoctorNameAndPatientName(
        specialty: String,
        doctorName: String,
        patientName: String,
        pageable: Pageable,
        status: PatientStatus? = null
    ): Page<Patient> {
        return if (status != null) {
            patientRepository.findByDoctorSpecialtyAndDoctorNameContainingIgnoreCaseAndPatientNameContainingIgnoreCaseAndStatus(
                specialty, doctorName, patientName, status, pageable
            )
        } else {
            patientRepository.findByDoctorSpecialtyAndDoctorNameContainingIgnoreCaseAndPatientNameContainingIgnoreCase(
                specialty, doctorName, patientName, pageable
            )
        }
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

    fun updatePatient(id: Long, editRequest: EditPatientRequest): Patient {
        val patient = patientRepository.findById(id)
            .orElseThrow { RuntimeException("Patient not found with id $id") }
        editRequest.name?.let { patient.name = it }
        editRequest.phone?.let { patient.phone = it }
        editRequest.status?.let { patient.status = it }
        return patientRepository.save(patient)
    }
}