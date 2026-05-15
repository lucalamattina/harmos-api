package ar.edu.itba.harmos.models

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "notification")
class Notification(
    @Column(nullable = false)
    val message: String,

    @Column(nullable = false)
    val read: Boolean = false,

    @Column(nullable = false)
    val date: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: AppUser,

    @Column(nullable = true)
    val announcementId: Long? = null,

    @Column(nullable = true)
    val reportId: Long? = null,

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1
) {
    constructor() : this("", false, LocalDateTime.now(), AppUser(), null, null, LocalDateTime.now(), LocalDateTime.now())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Notification
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
