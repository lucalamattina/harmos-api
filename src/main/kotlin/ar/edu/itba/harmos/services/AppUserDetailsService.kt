package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.models.Role
import ar.edu.itba.harmos.persistence.AppUserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AppUserDetailsService(val appUserRepository: AppUserRepository) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val user = appUserRepository.findByEmail(email) ?: throw UsernameNotFoundException(email)
        return User(user.email, user.password, getGrantedAuthorities(user.roles))
    }

    private fun getGrantedAuthorities(roles: Set<Role>): List<GrantedAuthority>? {
        val authorities = mutableListOf<GrantedAuthority>()
        for (role in roles) {
            authorities.add(SimpleGrantedAuthority(role.role))
        }
        return authorities
    }
}