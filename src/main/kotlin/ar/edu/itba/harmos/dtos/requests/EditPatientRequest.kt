package ar.edu.itba.harmos.dtos.requests

import ar.edu.itba.harmos.models.PatientStatus

data class EditPatientRequest(
    val name: String?,
    val phone: String?,
    val status: PatientStatus?
) 