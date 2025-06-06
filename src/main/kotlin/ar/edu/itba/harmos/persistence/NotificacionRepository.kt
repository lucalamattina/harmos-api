package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.Notification
import ar.edu.itba.harmos.models.AppUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUserAndReadFalse(user: AppUser): List<Notification>
    fun findByUser(user: AppUser): List<Notification>
} 