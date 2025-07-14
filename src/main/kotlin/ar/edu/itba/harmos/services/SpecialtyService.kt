package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateSpecialtyRequest
import ar.edu.itba.harmos.dtos.requests.EditSpecialtyRequest
import ar.edu.itba.harmos.models.Specialty
import ar.edu.itba.harmos.persistence.SpecialtyRepository
import ar.edu.itba.harmos.persistence.AppUserRepository
import ar.edu.itba.harmos.persistence.AnnouncementRepository
import org.springframework.stereotype.Service

@Service
class SpecialtyService(
    private val specialtyRepository: SpecialtyRepository,
    private val appUserRepository: AppUserRepository,
    private val announcementRepository: AnnouncementRepository
) {

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

    fun updateSpecialty(id: Long, editSpecialtyRequest: EditSpecialtyRequest): Specialty? {
        val specialtyOpt = specialtyRepository.findById(id)
        if (!specialtyOpt.isPresent) {
            return null
        }
        
        val currentSpecialty = specialtyOpt.get()
        
        // Only update name if provided (data field is not used in the model)
        val newName = editSpecialtyRequest.name ?: currentSpecialty.name
        
        // Check if name is already taken by another specialty
        if (newName != currentSpecialty.name) {
            val existingSpecialty = specialtyRepository.findByName(newName)
            if (existingSpecialty != null) {
                throw IllegalArgumentException("Specialty with name '$newName' already exists")
            }
        }
        
        // Create new instance with updated name (following LocationService pattern)
        val updatedSpecialty = Specialty(newName, id = currentSpecialty.id)
        return specialtyRepository.save(updatedSpecialty)
    }
    
}