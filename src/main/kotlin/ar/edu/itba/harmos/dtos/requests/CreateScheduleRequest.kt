package ar.edu.itba.harmos.dtos.requests

import java.time.DayOfWeek

data class CreateScheduleRequest(
        val dayOfWeek: DayOfWeek,
        val hourFrom: Int,
        val minuteFrom: Int,
        val hourTo: Int? = null,
        val minuteTo: Int? = null,
        val durationMinutes: Int? = null,
        val doctorUserId: Long,
        val patientId: Long
)
