package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateAppUserRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.AppUserRole
import ar.edu.itba.harmos.models.Role
import ar.edu.itba.harmos.models.Specialty
import ar.edu.itba.harmos.models.PasswordResetToken
import ar.edu.itba.harmos.persistence.AppUserRepository
import ar.edu.itba.harmos.persistence.RoleRepository
import ar.edu.itba.harmos.persistence.PasswordResetTokenRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.criteria.Predicate

@Service
class AppUserService(
    private val appUserRepository: AppUserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val specialtyService: SpecialtyService,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val emailService: EmailService,
    @Value("\${app.frontend.url}") private val frontendUrl: String
) {

    fun createUser(createAppUserRequest: CreateAppUserRequest): AppUser? {
        if (appUserRepository.findByEmail(createAppUserRequest.email) != null) {
            return null //TODO: dar feedback que el user existe
        }
        val resolvedSpecialties: MutableSet<Specialty> = if (createAppUserRequest.specialties.isNullOrEmpty()) {
            mutableSetOf()
        } else {
            try {
                createAppUserRequest.specialties!!.map { specialtyName ->
                    specialtyService.getSpecialtyByName(specialtyName)!!
                }.toMutableSet()
            } catch (e: IllegalArgumentException) {
                return null
            }
        }

        val rolesList = createAppUserRequest.roles ?: emptyList()
        var roles: MutableSet<Role> = rolesList.mapNotNull { roleNameRequest ->
            AppUserRole.fromRoleName(roleNameRequest)?.let { enumRole ->
                roleRepository.findByRole(enumRole.roleName)
            }
        }.toMutableSet()

        if (roles.isEmpty()) {
            roles = mutableSetOf(roleRepository.findByRole(AppUserRole.DOCTOR.roleName)!!)
        }

        val applicationUser = AppUser(
            createAppUserRequest.email,
            passwordEncoder.encode(createAppUserRequest.password),
            createAppUserRequest.firstName,
            createAppUserRequest.lastName,
            createAppUserRequest.phone,
            resolvedSpecialties,
            roles
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

    fun findUsersBySpecialties(specialties: List<Specialty>, page: Int, size: Int): List<AppUser> {
        return appUserRepository.findBySpecialtiesIn(specialties, PageRequest.of(page, size))
    }

    fun findAppUsersByEmailAndSpecialties(
        email: String?,
        name: String?,
        specialties: List<Specialty>?,
        page: Int,
        size: Int
    ): Page<AppUser> {
        val pageable = PageRequest.of(page, size, Sort.by("lastName"))
        val spec = Specification<AppUser> { root, query, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            email?.let {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%${it.lowercase()}%"))
            }

            name?.let {
                val nameParts = it.split(" ").map { part -> part.lowercase() }
                val namePredicates = mutableListOf<Predicate>()
                for (part in nameParts) {
                    namePredicates.add(
                        criteriaBuilder.or(
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%$part%"),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%$part%")
                        )
                    )
                }
                predicates.add(criteriaBuilder.and(*namePredicates.toTypedArray()))
            }

            specialties?.let {
                if (it.isNotEmpty()) {
                    predicates.add(root.get<Set<Specialty>>("specialties").`in`(it))
                }
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
        return appUserRepository.findAll(spec, pageable)
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

    @Transactional
    fun createPasswordResetTokenForUser(email: String): Boolean {
        val token = createPasswordResetToken(email) ?: return false
        
        // Send email with reset link
        val resetLink = "$frontendUrl/reset-password?token=$token"
        emailService.sendPasswordResetEmail(email, resetLink)
        
        return true
    }

    @Transactional
    fun createPasswordResetToken(email: String): String? {
        val user = appUserRepository.findByEmail(email) ?: return null
        
        // Delete any existing tokens for this user
        passwordResetTokenRepository.deleteByUserEmail(email)
        
        // Create new token
        val token = UUID.randomUUID().toString()
        val expiryDate = LocalDateTime.now().plusHours(24) // Token valid for 24 hours
        
        val passwordResetToken = PasswordResetToken(
            user = user,
            token = token,
            expiryDate = expiryDate
        )
        
        passwordResetTokenRepository.save(passwordResetToken)
        
        return token
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String): Boolean {
        val resetToken = passwordResetTokenRepository.findByToken(token) ?: return false
        
        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken)
            return false
        }
        
        val user = resetToken.user
        user.password = passwordEncoder.encode(newPassword)
        appUserRepository.save(user)
        
        // Delete the used token
        passwordResetTokenRepository.delete(resetToken)
        
        return true
    }

    fun validatePasswordResetToken(token: String): Boolean {
        val resetToken = passwordResetTokenRepository.findByToken(token) ?: return false
        return !resetToken.isExpired()
    }
}