package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.AppUserRole
import ar.edu.itba.harmos.models.Role
import ar.edu.itba.harmos.persistence.AppUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException

@ExtendWith(MockitoExtension::class)
class AppUserDetailsServiceTest {

    @Mock private lateinit var appUserRepository: AppUserRepository

    private lateinit var service: AppUserDetailsService

    @BeforeEach
    fun setUp() {
        service = AppUserDetailsService(appUserRepository)
    }

    private fun createUser(
        email: String = "user@example.com",
        roles: MutableSet<Role> = mutableSetOf()
    ) = AppUser(
        email = email,
        password = "encoded-password",
        firstName = "John",
        lastName = "Doe",
        phone = "123456789",
        roles = roles
    )

    @Test
    fun `loadUserByUsername returns UserDetails for existing user`() {
        // Given
        val doctorRole = Role(role = AppUserRole.DOCTOR.roleName, id = 1L)
        val user = createUser(roles = mutableSetOf(doctorRole))
        `when`(appUserRepository.findByEmail("user@example.com")).thenReturn(user)

        // When
        val userDetails = service.loadUserByUsername("user@example.com")

        // Then
        assertThat(userDetails.username).isEqualTo("user@example.com")
        assertThat(userDetails.authorities).contains(SimpleGrantedAuthority(AppUserRole.DOCTOR.roleName))
    }

    @Test
    fun `loadUserByUsername throws UsernameNotFoundException when user not found`() {
        // Given
        `when`(appUserRepository.findByEmail("unknown@example.com")).thenReturn(null)

        // When / Then
        assertThrows<UsernameNotFoundException> {
            service.loadUserByUsername("unknown@example.com")
        }
    }

    @Test
    fun `loadUserByUsername lowercases email before querying`() {
        // Given
        val user = createUser(email = "user@example.com")
        `when`(appUserRepository.findByEmail("user@example.com")).thenReturn(user)

        // When
        service.loadUserByUsername("USER@EXAMPLE.COM")

        // Then
        verify(appUserRepository).findByEmail("user@example.com")
    }

    @Test
    fun `loadUserByUsername maps multiple roles to authorities`() {
        // Given
        val doctorRole = Role(role = AppUserRole.DOCTOR.roleName, id = 1L)
        val adminRole = Role(role = AppUserRole.ADMINISTRATOR.roleName, id = 2L)
        val user = createUser(roles = mutableSetOf(doctorRole, adminRole))
        `when`(appUserRepository.findByEmail("user@example.com")).thenReturn(user)

        // When
        val userDetails = service.loadUserByUsername("user@example.com")

        // Then
        assertThat(userDetails.authorities).containsExactlyInAnyOrder(
            SimpleGrantedAuthority(AppUserRole.DOCTOR.roleName),
            SimpleGrantedAuthority(AppUserRole.ADMINISTRATOR.roleName)
        )
    }
}
