package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Schedule
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ScheduleRepository : CrudRepository<Schedule, Long> {
    fun findByDoctor(doctor: AppUser): Set<Schedule>
}