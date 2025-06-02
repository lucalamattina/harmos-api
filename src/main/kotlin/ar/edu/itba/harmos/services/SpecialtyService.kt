package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateSpecialtyRequest
import ar.edu.itba.harmos.models.Specialty
import ar.edu.itba.harmos.persistence.SpecialtyRepository
import org.springframework.stereotype.Service

@Service
class SpecialtyService(private val specialtyRepository: SpecialtyRepository) {

    fun createSpecialty(createSpecialtyRequest: CreateSpecialtyRequest): Specialty? {
        if (specialtyRepository.findByName(createSpecialtyRequest.name) != null) {
            return null
        }
        val applicationSpecialty = Specialty(createSpecialtyRequest.name)
        return specialtyRepository.save(applicationSpecialty)
    }

    fun getSpecialtyByName(name: String): Specialty? {
        return specialtyRepository.findByName(name)?: throw IllegalArgumentException("Specialty not found")
    }

    fun getSpecialtyById(id: Long): Specialty? {
        val opt = specialtyRepository.findById(id)
        if (opt.isPresent) {
            return opt.get()
        }
        return null
    }

    fun getAllSpecialties(): List<Specialty> {
        return specialtyRepository.findAll()
    }

    fun deletePatientById(id: Long): Boolean {
        val specialtyOpt = specialtyRepository.findById(id)
        if (!specialtyOpt.isPresent) {
            return false
        }
        val specialty = specialtyOpt.get()
        // Remover de usuarios
        val users = ar.edu.itba.harmos.persistence.AppUserRepository::class.java.declaredFields
        // Remover de anuncios
        // Obtener beans de repositorios
        val appContext = org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext()
        val appUserRepository = appContext.getBean(ar.edu.itba.harmos.persistence.AppUserRepository::class.java)
        val announcementRepository = appContext.getBean(ar.edu.itba.harmos.persistence.AnnouncementRepository::class.java)
        // Remover de usuarios
        appUserRepository.findAll().forEach { user ->
            if (user.specialties.remove(specialty)) {
                appUserRepository.save(user)
            }
        }
        // Remover de anuncios
        announcementRepository.findAll().forEach { announcement ->
            if (announcement.specialties.remove(specialty)) {
                announcementRepository.save(announcement)
            }
        }
        specialtyRepository.delete(specialty)
        return true
    }
    
}