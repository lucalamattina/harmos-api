package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateAppUserRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.persistence.AppUserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Service
class AppUserService(
    private val appUserRepository: AppUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val specialtyService: SpecialtyService) {

    fun createUser(createAppUserRequest: CreateAppUserRequest): AppUser? {
        if (appUserRepository.findByEmail(createAppUserRequest.email) != null) {
            return null //TODO: dar feedback que el user existe
        }
        val specialties = createAppUserRequest.specialties.mapNotNull { specialtyName ->
            specialtyService.getSpecialtyByName(specialtyName)
        }.toMutableSet()

        if (specialties.isEmpty()) {
            throw IllegalArgumentException("At least one valid specialty must be provided")
        }
        val applicationUser = AppUser(
            createAppUserRequest.email,
            passwordEncoder.encode(createAppUserRequest.password),
            createAppUserRequest.firstName,
            createAppUserRequest.lastName,
            createAppUserRequest.phone,
            specialties,
            emptySet() //TODO:SETEO DE ROLES
        )
        return appUserRepository.save(applicationUser)
    }

    fun getAppUserByEmail(email: String): AppUser? {
        return appUserRepository.findByEmail(email)
    }

    fun getAppUserById(id: Long): AppUser? {
        val opt = appUserRepository.findById(id)
        if (opt.isPresent) {
            return opt.get()
        }
        return null
    }


    fun findUsersBySpecialties(specialties: List<String>, page: Int, size: Int): List<AppUser> {
        return appUserRepository.findBySpecialtiesIn(specialties, PageRequest.of(page, size))
    }

    fun findAllUsers(page: Int, size: Int): List<AppUser> {
        return appUserRepository.findAll(PageRequest.of(page, size)).content
    }

    fun findUserBySpecialtyAndIdOrEmail(specialties: List<String>, id: Long?, email: String?): List<AppUser> {
        return appUserRepository.findBySpecialtiesInAndIdOrEmail(specialties, id, email)
    }

    fun findUserByIdOrEmail(id: Long?, email: String?): List<AppUser> {
        return when {
            id != null -> appUserRepository.findById(id).map { listOf(it) }.orElse(emptyList())
            email != null -> appUserRepository.findByEmail(email)?.let { listOf(it) } ?: emptyList()  //TODO:QUE DEVUELVA CHEQUENADO que sean iguales
            else -> emptyList()
        }
    }

    fun findAppUsersByEmailAndSpecialties(
        email: String? = null,
        specialties: List<String>? = null,
        page: Int = 0,
        size: Int = 10
    ): Page<AppUser> {
        val pageable: Pageable = PageRequest.of(page, size)
        return appUserRepository.findAppUsersByEmailAndSpecialties(email, specialties, pageable)
    }

    fun deleteUserById(id: Long): Boolean {
        val user = appUserRepository.findById(id)
        return if (user.isPresent) {
            appUserRepository.delete(user.get())
            true
        } else {
            false
        }
    }

    fun addSpecialtyToUser(userId: Long, specialtyId: Long): Boolean {
        val user = appUserRepository.findById(userId).orElse(null) ?: return false
        val specialty = specialtyService.getSpecialtyById(specialtyId) ?: return false

        user.specialties.add(specialty)
        appUserRepository.save(user)
        return true
    }
}