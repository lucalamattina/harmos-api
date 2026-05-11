package ar.edu.itba.harmos.dtos.requests

import ar.edu.itba.harmos.models.PatientStatus
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

data class CreatePatientRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 255, message = "First name must not exceed 255 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 255, message = "Last name must not exceed 255 characters")
    val lastName: String,

    @field:NotBlank(message = "Phone is required")
    @field:Size(max = 50, message = "Phone must not exceed 50 characters")
    val phone: String,

    @field:NotNull(message = "Status is required")
    val status: PatientStatus = PatientStatus.PENDING,

    val doctorIds: List<Long>? = null
)
