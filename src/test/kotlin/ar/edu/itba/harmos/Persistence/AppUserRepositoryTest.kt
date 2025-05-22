package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Specialty
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.assertj.core.api.Assertions.assertThat

@DataJpaTest
class AppUserRepositoryTest {

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var appUserRepository: AppUserRepository

    private fun createSpecialty(name: String): Specialty {
        val specialty = Specialty(name = name)
        entityManager.persist(specialty)
        return specialty
    }

    private fun createAppUser(email: String, specialties: MutableSet<Specialty>): AppUser {
        val user = AppUser(
            email = email,
            password = "password",
            firstName = "John",
            lastName = "Doe",
            phone = "123-456-7890",
            specialties = specialties,
            roles = emptySet()
        )
        entityManager.persist(user)
        return user
    }

    @Test
    fun `findAppUsersByEmailAndSpecialties should return users matching email like`() {
        // Given
        val specialty1 = createSpecialty("Cardiología")
        val specialty2 = createSpecialty("Neurología")
        val user1 = createAppUser("tdorado1@example.com", mutableSetOf(specialty1))
        createAppUser("otro@example.com", mutableSetOf(specialty2)) // Not used in assertions
        val user3 = createAppUser("tdorado2@test.com", mutableSetOf(specialty1, specialty2))
        entityManager.flush()

        val pageable: Pageable = PageRequest.of(0, 10)

        // When
        val result = appUserRepository.findAppUsersByEmailAndSpecialties(
            email = "tdorado",
            name = null,
            specialties = null,
            pageable = pageable
        )

        // Then
        assertThat(result.content).hasSize(2)
        assertThat(result.content).extracting("email").containsExactlyInAnyOrder(user1.email, user3.email)
    }

    @Test
    fun `findAppUsersByEmailAndSpecialties should return users matching specialty`() {
        // Given
        val specialty1 = createSpecialty("Cardiología")
        val specialty2 = createSpecialty("Neurología")
        val user1 = createAppUser("user1@example.com", mutableSetOf(specialty1))
        createAppUser("user2@example.com", mutableSetOf(specialty2)) // Not used in assertions
        val user3 = createAppUser("user3@test.com", mutableSetOf(specialty1, specialty2))
        entityManager.flush()

        val pageable: Pageable = PageRequest.of(0, 10)

        // When
        val result = appUserRepository.findAppUsersByEmailAndSpecialties(
            email = null,
            name = null,
            specialties = listOf(specialty1),
            pageable = pageable
        )

        // Then
        assertThat(result.content).hasSize(2)
        assertThat(result.content).extracting("email").containsExactlyInAnyOrder(user1.email, user3.email)
    }

    @Test
    fun `findAppUsersByEmailAndSpecialties should return users matching both email like and specialty`() {
        // Given
        val specialty1 = createSpecialty("Cardiología")
        val specialty2 = createSpecialty("Neurología")
        val user1 = createAppUser("tdorado1@example.com", mutableSetOf(specialty1))
        createAppUser("otro@example.com", mutableSetOf(specialty2)) // Not used in assertions
        val user3 = createAppUser("tdorado2@test.com", mutableSetOf(specialty1, specialty2))
        entityManager.flush()

        val pageable: Pageable = PageRequest.of(0, 10)

        // When
        val result = appUserRepository.findAppUsersByEmailAndSpecialties(
            email = "tdorado",
            name = null,
            specialties = listOf(specialty1),
            pageable = pageable
        )

        // Then
        assertThat(result.content).hasSize(2)
        assertThat(result.content).extracting("email").containsExactlyInAnyOrder(user1.email, user3.email)
    }

    @Test
    fun `findAppUsersByEmailAndSpecialties should return all users when email and specialties are null`() {
        // Given
        val specialty1 = createSpecialty("Cardiología")
        val specialty2 = createSpecialty("Neurología")
        val user1 = createAppUser("user1@example.com", mutableSetOf(specialty1))
        val user2 = createAppUser("user2@example.com", mutableSetOf(specialty2))
        entityManager.flush()

        val pageable: Pageable = PageRequest.of(0, 10)

        // When
        val result = appUserRepository.findAppUsersByEmailAndSpecialties(
            email = null,
            name = null,
            specialties = null,
            pageable = pageable
        )

        // Then
        assertThat(result.content).hasSize(2)
        assertThat(result.content).extracting("email").containsExactlyInAnyOrder(user1.email, user2.email)
    }

    @Test
    fun `findAppUsersByEmailAndSpecialties should return empty page when no matching users are found`() {
        // Given
        val specialty1 = createSpecialty("Cardiología")
        createAppUser("user1@example.com", mutableSetOf(specialty1))
        entityManager.flush()

        val pageable: Pageable = PageRequest.of(0, 10)

        // When
        val result = appUserRepository.findAppUsersByEmailAndSpecialties(
            email = "nonexistent",
            name = null,
            specialties = listOf(createSpecialty("Pediatría")),
            pageable = pageable
        )

        // Then
        assertThat(result.content).isEmpty()
    }

    @Test
    fun `findAppUsersByEmailAndSpecialties should apply pagination correctly`() {
        // Given
        val specialty = createSpecialty("Traumatología")
        for (i in 1..5) {
            createAppUser("user$i@example.com", mutableSetOf(specialty))
        }
        entityManager.flush()

        val pageableFirstPage: Pageable = PageRequest.of(0, 2)
        val pageableSecondPage: PageRequest = PageRequest.of(1, 2)

        // When
        val firstPageResult = appUserRepository.findAppUsersByEmailAndSpecialties(
            email = null,
            name = null,
            specialties = listOf(specialty),
            pageable = pageableFirstPage
        )
        val secondPageResult = appUserRepository.findAppUsersByEmailAndSpecialties(
            email = null,
            name = null,
            specialties = listOf(specialty),
            pageable = pageableSecondPage
        )

        // Then
        assertThat(firstPageResult.content).hasSize(2)
        assertThat(secondPageResult.content).hasSize(2)
        assertThat(firstPageResult.totalElements).isEqualTo(5)
        assertThat(firstPageResult.totalPages).isEqualTo(3) // Ceiling of 5 / 2
    }
}