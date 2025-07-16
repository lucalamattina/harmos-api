package ar.edu.itba.harmos.dtos.requests

data class EditReportRequest(
    val title: String? = null,
    val patientId: Long? = null,
    val specialtyId: Long? = null
) 