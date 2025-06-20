package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.models.Schedule
import java.time.DayOfWeek

data class ScheduleResponse(
        val id: Long,
        val locationId: Long,
        val dayOfWeek: DayOfWeek,
        val hourFrom: Int,
        val minuteFrom: Int,
        val hourTo: Int,
        val minuteTo: Int,
        val doctorUserId: Long,
        val patientId: Long
) {
    companion object {
        fun singleFromModel(schedule: Schedule): ScheduleResponse {
            return ScheduleResponse(
                    schedule.id,
                    schedule.location.id,
                    schedule.dayOfWeek,
                    schedule.hourFrom,
                    schedule.minuteFrom,
                    schedule.hourTo,
                    schedule.minuteTo,
                    schedule.doctor.id,
                    schedule.patient.id
            )
        }
        fun setFromModel(schedules: Set<Schedule>): Set<ScheduleResponse> {
            return schedules.map { singleFromModel(it) }.toSet()
        }
    }
}
