package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateScheduleRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Location
import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.models.Schedule
import ar.edu.itba.harmos.persistence.ScheduleRepository
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import javax.persistence.criteria.Predicate

@Service
class ScheduleService(private val scheduleRepository: ScheduleRepository) {
    fun createSchedule(
            createScheduleRequest: CreateScheduleRequest,
            doctor: AppUser,
            patient: Patient,
            location: Location
    ): Schedule {
        val schedule =
                Schedule(
                        location,
                        createScheduleRequest.dayOfWeek,
                        createScheduleRequest.hourFrom,
                        createScheduleRequest.minuteFrom,
                        createScheduleRequest.hourTo,
                        createScheduleRequest.minuteTo,
                        doctor,
                        patient
                )
        return scheduleRepository.save(schedule)
    }

    fun findAll(): Set<Schedule> {
        return scheduleRepository.findAll().toSet()
    }

    fun findAllByDoctor(doctor: AppUser): Set<Schedule> {
        return scheduleRepository.findByDoctor(doctor)
    }

    fun findFilteredSchedules(
        locationId: Long?,
        doctorId: Long?,
        patientId: Long?
    ): Set<Schedule> {
        val spec = Specification<Schedule> { root, query, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            locationId?.let {
                predicates.add(criteriaBuilder.equal(root.get<Location>("location").get<Long>("id"), it))
            }

            doctorId?.let {
                predicates.add(criteriaBuilder.equal(root.get<AppUser>("doctor").get<Long>("id"), it))
            }

            patientId?.let {
                predicates.add(criteriaBuilder.equal(root.get<Patient>("patient").get<Long>("id"), it))
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
        return scheduleRepository.findAll(spec).toSet()
    }

    fun deleteScheduleById(id: Long): Boolean {
        val schedule = scheduleRepository.findById(id)
        return if (schedule.isPresent) {
            scheduleRepository.delete(schedule.get())
            true
        } else {
            false
        }
    }
}
