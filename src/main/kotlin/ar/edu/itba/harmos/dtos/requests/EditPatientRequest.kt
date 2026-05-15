package ar.edu.itba.harmos.dtos.requests

import ar.edu.itba.harmos.models.PatientStatus
import javax.validation.constraints.Size

data class EditPatientRequest(
    @field:Size(max = 255, message = "First name must not exceed 255 characters")
    val firstName: String?,

    @field:Size(max = 255, message = "Last name must not exceed 255 characters")
    val lastName: String?,

    @field:Size(max = 50, message = "Phone must not exceed 50 characters")
    val phone: String?,

    val status: PatientStatus?,

    // If present, replaces the entire doctors list (empty list removes all doctors).
    val doctorIds: List<Long>?
)
