package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.models.PatientStatus

data class PatientResponse(
    val id: Long,
    val name: String,
    val phone: String,
    val status: PatientStatus,
    val doctors: List<AppUserResponse>,
    val doctorSpecialties: Set<String>
) {
    companion object {
        fun singleFromModel(patient: Patient): PatientResponse {
            return PatientResponse(
                patient.id,
                patient.name,
                patient.phone,
                patient.status,
                patient.doctors.map { AppUserResponse.singleFromModel(it) },
                patient.doctors.flatMap { it.specialties }.map { it.name }.toSet()
            )
        }
    }
}