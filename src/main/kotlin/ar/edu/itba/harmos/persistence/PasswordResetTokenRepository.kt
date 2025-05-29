package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByToken(token: String): PasswordResetToken?
    fun findByUserEmail(email: String): PasswordResetToken?
    fun deleteByUserEmail(email: String)
} 