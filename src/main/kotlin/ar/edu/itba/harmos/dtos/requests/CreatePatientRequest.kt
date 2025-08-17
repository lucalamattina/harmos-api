package ar.edu.itba.harmos.dtos.requests

import ar.edu.itba.harmos.models.PatientStatus

data class CreatePatientRequest(
    val firstName: String,
    val lastName: String,
    val phone: String,
    val status: PatientStatus = PatientStatus.PENDING
)
