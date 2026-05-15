package ar.edu.itba.harmos.dtos.requests

import javax.validation.constraints.Positive
import javax.validation.constraints.Size

data class EditReportRequest(
    @field:Size(max = 255, message = "Title must not exceed 255 characters")
    val title: String? = null,

    @field:Positive(message = "Patient ID must be a positive number")
    val patientId: Long? = null,

    @field:Positive(message = "Specialty ID must be a positive number")
    val specialtyId: Long? = null
)
