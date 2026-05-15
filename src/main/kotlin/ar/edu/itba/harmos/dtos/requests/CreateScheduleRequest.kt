package ar.edu.itba.harmos.dtos.requests

import java.time.DayOfWeek
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive

data class CreateScheduleRequest(
    @field:NotNull(message = "Day of week is required")
    val dayOfWeek: DayOfWeek,

    @field:NotNull(message = "Start hour is required")
    @field:Min(0, message = "Start hour must be between 0 and 23")
    @field:Max(23, message = "Start hour must be between 0 and 23")
    val hourFrom: Int,

    @field:NotNull(message = "Start minute is required")
    @field:Min(0, message = "Start minute must be between 0 and 59")
    @field:Max(59, message = "Start minute must be between 0 and 59")
    val minuteFrom: Int,

    @field:Min(0, message = "End hour must be between 0 and 23")
    @field:Max(23, message = "End hour must be between 0 and 23")
    val hourTo: Int? = null,

    @field:Min(0, message = "End minute must be between 0 and 59")
    @field:Max(59, message = "End minute must be between 0 and 59")
    val minuteTo: Int? = null,

    @field:Positive(message = "Duration must be a positive number of minutes")
    val durationMinutes: Int? = null,

    @field:NotNull(message = "Doctor user ID is required")
    @field:Positive(message = "Doctor user ID must be a positive number")
    val doctorUserId: Long,

    @field:NotNull(message = "Patient ID is required")
    @field:Positive(message = "Patient ID must be a positive number")
    val patientId: Long
)
