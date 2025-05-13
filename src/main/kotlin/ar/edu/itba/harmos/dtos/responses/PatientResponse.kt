package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.models.Patient
import org.springframework.data.domain.Page

data class PatientResponse (
    val id: Long,
    val name: String,
    val phone: String,
    val status: String
) {
    companion object {
        fun singleFromModel(patient: Patient) : PatientResponse {
            return PatientResponse(
                patient.id,
                patient.name,
                patient.phone,
                patient.status
            )
        }
        fun listFromModel(patients: List<Patient>): List<PatientResponse> {
            return patients.map { singleFromModel(it) }
        }
        fun pageFromModel(patients: Page<Patient>): Page<PatientResponse> {
            return patients.map { singleFromModel(it) }
        }
    }
}

data class PatientDetailResponse(
    val id: Long,
    val name: String,
    val phone: String,
    val status: String,
    val doctors: List<AppUserResponse>
) {
    companion object {
        fun fromModel(patient: Patient) : PatientDetailResponse {
            return PatientDetailResponse(
                patient.id,
                patient.name,
                patient.phone,
                patient.status,
                patient.doctors.map { AppUserResponse.singleFromModel(it) }
            )
        }
    }
}