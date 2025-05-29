package ar.edu.itba.harmos.models

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetToken(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: AppUser,

    @Column(nullable = false)
    var token: String,

    @Column(nullable = false)
    var expiryDate: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = -1
) {
    constructor() : this(
        user = AppUser(),
        token = "",
        expiryDate = LocalDateTime.now()
    )

    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiryDate)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PasswordResetToken

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
} 