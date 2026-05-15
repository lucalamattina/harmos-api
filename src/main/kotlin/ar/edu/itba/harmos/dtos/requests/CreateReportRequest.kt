package ar.edu.itba.harmos.dtos.requests

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive
import javax.validation.constraints.Size

data class CreateReportRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 255, message = "Title must not exceed 255 characters")
    val title: String,

    @field:NotNull(message = "Patient ID is required")
    @field:Positive(message = "Patient ID must be a positive number")
    val patientId: Long,

    @field:NotNull(message = "Specialty ID is required")
    @field:Positive(message = "Specialty ID must be a positive number")
    val specialtyId: Long
)
