package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.models.Schedule
import java.time.DayOfWeek

// DTO anidado para la información del doctor
data class DoctorScheduleResponse(val id: Long, val firstName: String, val lastName: String)

// DTO anidado para la información del paciente
data class PatientScheduleResponse(val id: Long, val firstName: String, val lastName: String)

data class ScheduleResponse(
        val id: Long,
        val dayOfWeek: DayOfWeek,
        val hourFrom: Int,
        val minuteFrom: Int,
        val hourTo: Int,
        val minuteTo: Int,
        val doctor: DoctorScheduleResponse,
        val patient: PatientScheduleResponse
) {
    companion object {
        fun singleFromModel(schedule: Schedule): ScheduleResponse {
            val doctorResponse = DoctorScheduleResponse(
                    id = schedule.doctor.id,
                    firstName = schedule.doctor.firstName,
                    lastName = schedule.doctor.lastName
            )
            val patientResponse = PatientScheduleResponse(
                    id = schedule.patient.id,
                    firstName = schedule.patient.firstName,
                    lastName = schedule.patient.lastName
            )
            return ScheduleResponse(
                    schedule.id,
                    schedule.dayOfWeek,
                    schedule.hourFrom,
                    schedule.minuteFrom,
                    schedule.hourTo,
                    schedule.minuteTo,
                    doctor = doctorResponse,
                    patient = patientResponse
            )
        }

        fun setFromModel(schedules: Set<Schedule>): Set<ScheduleResponse> {
            return schedules.map { singleFromModel(it) }.toSet()
        }
    }
}
