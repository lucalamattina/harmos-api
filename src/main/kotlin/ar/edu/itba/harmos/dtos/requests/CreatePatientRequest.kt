package ar.edu.itba.harmos.dtos.requests

import ar.edu.itba.harmos.models.PatientStatus
import com.fasterxml.jackson.annotation.JsonProperty

data class CreatePatientRequest(
    val firstName: String,
    val lastName: String,
    val phone: String,
    val status: PatientStatus = PatientStatus.PENDING,
    @JsonProperty("doctor_ids")
    val doctorIds: List<Long>? = null
)
