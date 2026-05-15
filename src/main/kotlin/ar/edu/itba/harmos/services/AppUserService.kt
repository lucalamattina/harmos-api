package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateAppUserRequest
import ar.edu.itba.harmos.dtos.requests.EditAppUserRequest
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
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(AppUserService::class.java)

    @Transactional
    fun createUser(createAppUserRequest: CreateAppUserRequest): AppUser {
        if (appUserRepository.findByEmail(createAppUserRequest.email) != null) {
            throw IllegalArgumentException("El email ya se encuentra registrado")
        }
        val resolvedSpecialty: Specialty? = if (createAppUserRequest.specialty.isNullOrEmpty()) {
            null
        } else {
            specialtyService.getSpecialtyByName(createAppUserRequest.specialty)
                ?: throw IllegalArgumentException("La especialidad '${createAppUserRequest.specialty}' no existe")
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
            resolvedSpecialty,
            roles
        )
        return appUserRepository.save(applicationUser)
    }

    @Transactional(readOnly = true)
    fun getAppUserByEmail(email: String): AppUser? {
        return appUserRepository.findByEmail(email)
    }

    /**
     * Returns the role names for a user inside a transactional boundary so
     * the LAZY `AppUser.roles` collection can be safely initialized when
     * called from non-transactional contexts (e.g. servlet filters).
     */
    @Transactional(readOnly = true)
    fun getRoleNamesByEmail(email: String): List<String>? {
        val user = appUserRepository.findByEmail(email) ?: return null
        return user.roles.map { it.role }
    }

    @Transactional(readOnly = true)
    fun getAppUserById(id: Long): AppUser? {
        val opt = appUserRepository.findById(id)
        if (opt.isPresent) {
            return opt.get()
        }
        return null
    }

    @Transactional(readOnly = true)
    fun findUsersBySpecialties(specialties: List<Specialty>, page: Int, size: Int): List<AppUser> {
        return appUserRepository.findBySpecialtyIn(specialties, PageRequest.of(page, size))
    }

    @Transactional(readOnly = true)
    fun findAppUsersByEmailAndSpecialties(
        email: String?,
        name: String?,
        specialties: List<String>?,
        page: Int,
        size: Int
    ): Page<AppUser> {
        val pageable = PageRequest.of(page, size, Sort.by("lastName"))
        val spec = Specification<AppUser> { root, _, criteriaBuilder ->
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

            specialties?.let { specialtyNames ->
                if (specialtyNames.isNotEmpty()) {
                    val specialtyJoin = root.join<AppUser, Specialty>("specialty")
                    predicates.add(specialtyJoin.get<String>("name").`in`(specialtyNames))
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

        user.specialty = specialty
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

    @Transactional
    fun updateUser(id: Long, editAppUserRequest: EditAppUserRequest): AppUser? {
        val user = appUserRepository.findById(id).orElse(null) ?: return null

        // Update basic fields if provided
        editAppUserRequest.firstName?.let { user.firstName = it.trim() }
        editAppUserRequest.lastName?.let { user.lastName = it.trim() }
        editAppUserRequest.phone?.let { user.phone = it.trim() }

        // Update specialty if provided (empty string clears it)
        editAppUserRequest.specialty?.let { specialtyName ->
            user.specialty = if (specialtyName.isBlank()) {
                null
            } else {
                specialtyService.getSpecialtyByName(specialtyName.trim()) ?: return null
            }
        }

        // Update roles if provided
        editAppUserRequest.roles?.let { roleNames ->
            val resolvedRoles = roleNames.mapNotNull { roleName ->
                AppUserRole.fromRoleName(roleName.trim())?.let { enumRole ->
                    roleRepository.findByRole(enumRole.roleName)
                }
            }.toMutableSet()
            
            // If no valid roles provided, keep current roles
            if (resolvedRoles.isNotEmpty()) {
                user.roles.clear()
                user.roles.addAll(resolvedRoles)
            }
        }

        return appUserRepository.save(user)
    }
}