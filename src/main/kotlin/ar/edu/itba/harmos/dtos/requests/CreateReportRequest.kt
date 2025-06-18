package ar.edu.itba.harmos.dtos.requests

data class CreateReportRequest(
    val title: String,
    val patientId: Long,
    val specialtyId: Long
) 