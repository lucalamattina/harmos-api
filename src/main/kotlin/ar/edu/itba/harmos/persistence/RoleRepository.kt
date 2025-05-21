package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<Role, Long> {
    fun findByRole(role: String): Role?
}