package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreatePatientRequest
import ar.edu.itba.harmos.dtos.requests.EditPatientRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.models.PatientStatus
import ar.edu.itba.harmos.models.Specialty
import ar.edu.itba.harmos.persistence.AppUserRepository
import ar.edu.itba.harmos.persistence.PatientRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.criteria.Join
import javax.persistence.criteria.Predicate

@Service
class PatientService(
    private val patientRepository: PatientRepository,
    private val appUserRepository: AppUserRepository
) {

    fun createPatient(createPatientRequest: CreatePatientRequest): Patient {
        val doctors = createPatientRequest.doctorIds?.let {
            appUserRepository.findAllById(it)
        } ?: mutableListOf()

        val patient = Patient(
            firstName = createPatientRequest.firstName,
            lastName = createPatientRequest.lastName,
            phone = createPatientRequest.phone,
            status = createPatientRequest.status,
            doctors = doctors.toMutableList(),
            reports = emptyList()
        )
        return patientRepository.save(patient)
    }

    fun getPatients(
        name: String?,
        doctor: String?,
        specialty: String?,
        status: PatientStatus?,
        doctorId: Long?,
        pageable: Pageable
    ): Page<Patient> {
        val spec = Specification<Patient> { root, criteriaQuery, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()
            // Use distinct to avoid duplicates when joining with doctors
            criteriaQuery.distinct(true)

            name?.let {
                val nameParts = it.split(" ").filter { part -> part.isNotBlank() }.map { part -> part.lowercase() }
                if (nameParts.isNotEmpty()) {
                    val namePredicates = mutableListOf<Predicate>()
                    for (part in nameParts) {
                        namePredicates.add(
                            criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%$part%"),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%$part%")
                            )
                        )
                    }
                    predicates.add(criteriaBuilder.and(*namePredicates.toTypedArray()))
                }
            }

            if (doctor != null || specialty != null || doctorId != null) {
                val doctorsJoin: Join<Patient, AppUser> = root.join("doctors")

                doctor?.let {
                    val nameParts = it.split(" ").filter { part -> part.isNotBlank() }.map { part -> part.lowercase() }
                    if (nameParts.isNotEmpty()) {
                        val doctorNamePredicates = mutableListOf<Predicate>()
                        for (part in nameParts) {
                            doctorNamePredicates.add(
                                criteriaBuilder.or(
                                    criteriaBuilder.like(criteriaBuilder.lower(doctorsJoin.get("firstName")), "%$part%"),
                                    criteriaBuilder.like(criteriaBuilder.lower(doctorsJoin.get("lastName")), "%$part%")
                                )
                            )
                        }
                        predicates.add(criteriaBuilder.and(*doctorNamePredicates.toTypedArray()))
                    }
                }

                specialty?.let {
                    val specialtiesJoin: Join<AppUser, Specialty> = doctorsJoin.join("specialties")
                    predicates.add(criteriaBuilder.equal(specialtiesJoin.get<String>("name"), it))
                }

                doctorId?.let {
                    predicates.add(criteriaBuilder.equal(doctorsJoin.get<Long>("id"), it))
                }
            }

            status?.let {
                predicates.add(criteriaBuilder.equal(root.get<PatientStatus>("status"), it))
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
        return patientRepository.findAll(spec, pageable)
    }

    fun getPatientById(id: Long): Patient? {
        return patientRepository.findById(id).orElse(null)
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

    @Transactional
    fun addDoctorToPatient(patientId: Long, doctorId: Long): Boolean {
        val patientOpt = patientRepository.findById(patientId)
        val doctorOpt = appUserRepository.findById(doctorId)

        if (patientOpt.isPresent && doctorOpt.isPresent) {
            val patient = patientOpt.get()
            val doctor = doctorOpt.get()

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

        editRequest.firstName?.let { patient.firstName = it }
        editRequest.lastName?.let { patient.lastName = it }
        editRequest.phone?.let { patient.phone = it }
        editRequest.status?.let { patient.status = it }

        return patientRepository.save(patient)
    }
}