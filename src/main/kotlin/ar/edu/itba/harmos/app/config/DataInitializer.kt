package ar.edu.itba.harmos.app.config

import ar.edu.itba.harmos.models.Role
import ar.edu.itba.harmos.models.AppUserRole
import ar.edu.itba.harmos.persistence.RoleRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class DataInitializer(private val roleRepository: RoleRepository) : CommandLineRunner {
    override fun run(vararg args: String?) {
        AppUserRole.values().forEach { userRoleEnum ->
            if (roleRepository.findByRole(userRoleEnum.roleName) == null) {
                roleRepository.save(Role(userRoleEnum.roleName))
            }
        }
    }
}