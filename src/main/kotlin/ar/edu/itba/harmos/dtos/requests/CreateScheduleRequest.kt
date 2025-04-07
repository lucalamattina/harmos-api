package ar.edu.itba.harmos.dtos.requests

import java.time.DayOfWeek

data class CreateScheduleRequest(
    val location: String,
    val dayOfWeek: DayOfWeek,
    val hourFrom: Int,
    val minuteFrom: Int,
    val hourTo: Int,
    val minuteTo: Int,
    val doctorUserId: Long
)