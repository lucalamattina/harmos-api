package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.models.Patient

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
    }
}