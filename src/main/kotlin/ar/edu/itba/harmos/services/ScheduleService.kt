package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateScheduleRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Schedule
import ar.edu.itba.harmos.persistence.ScheduleRepository
import org.springframework.stereotype.Service

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository
) {
    fun createSchedule(createScheduleRequest: CreateScheduleRequest, doctor: AppUser): Schedule? {
        val schedule = Schedule(
            createScheduleRequest.location,
            createScheduleRequest.dayOfWeek,
            createScheduleRequest.hourFrom,
            createScheduleRequest.minuteFrom,
            createScheduleRequest.hourTo,
            createScheduleRequest.minuteTo,
            doctor
        )
        return scheduleRepository.save(schedule)
    }

    fun findAll(): Set<Schedule> {
        return scheduleRepository.findAll().toSet()
    }

    fun findAllByDoctor(doctor: AppUser): Set<Schedule> {
        return scheduleRepository.findByDoctor(doctor)
    }
}