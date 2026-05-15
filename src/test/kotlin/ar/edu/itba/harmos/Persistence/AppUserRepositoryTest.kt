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
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mail.javamail.JavaMailSender

@DataJpaTest
class AppUserRepositoryTest {

    // @DataJpaTest loads @Configuration beans including SecurityConfiguration which
    // transitively needs EmailService → JavaMailSender (not auto-configured in JPA slice).
    @MockBean private lateinit var javaMailSender: JavaMailSender

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var appUserRepository: AppUserRepository

    private fun createSpecialty(name: String): Specialty {
        val specialty = Specialty(name = name)
        entityManager.persist(specialty)
        return specialty
    }

    private fun createAppUser(email: String, specialty: Specialty? = null): AppUser {
        val user = AppUser(
            email = email,
            password = "password",
            firstName = "John",
            lastName = "Doe",
            phone = "123-456-7890",
            specialty = specialty,
            roles = mutableSetOf()
        )
        entityManager.persist(user)
        return user
    }

    @Test
    fun `findAppUsersByEmailAndSpecialties should return users matching email like`() {
        // Given
        val specialty1 = createSpecialty("Cardiología")
        val specialty2 = createSpecialty("Neurología")
        val user1 = createAppUser("tdorado1@example.com", specialty1)
        createAppUser("otro@example.com", specialty2)
        val user3 = createAppUser("tdorado2@test.com", specialty1)
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
        val user1 = createAppUser("user1@example.com", specialty1)
        createAppUser("user2@example.com", specialty2)
        val user3 = createAppUser("user3@test.com", specialty1)
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
        val user1 = createAppUser("tdorado1@example.com", specialty1)
        createAppUser("otro@example.com", specialty2)
        val user3 = createAppUser("tdorado2@test.com", specialty1)
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
        val user1 = createAppUser("user1@example.com", specialty1)
        val user2 = createAppUser("user2@example.com", specialty2)
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
        createAppUser("user1@example.com", specialty1)
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
            createAppUser("user$i@example.com", specialty)
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
