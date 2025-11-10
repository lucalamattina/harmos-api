package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.Notification
import ar.edu.itba.harmos.models.AppUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findByUserAndReadFalse(user: AppUser): List<Notification>

    fun findByUser(user: AppUser): List<Notification>

    // Ordenar por fecha descendente (más recientes primero)
    @Query("""
        SELECT n FROM Notification n 
        WHERE n.user = :user AND n.read = false 
        ORDER BY n.date DESC
    """)
    fun findByUserAndReadFalseOrderByDateDesc(@Param("user") user: AppUser): List<Notification>

    @Query("""
        SELECT n FROM Notification n 
        WHERE n.user = :user 
        ORDER BY n.date DESC
    """)
    fun findByUserOrderByDateDesc(@Param("user") user: AppUser): List<Notification>
} 