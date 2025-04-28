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
        val specialty = specialtyRepository.findById(id)
        return if (specialty.isPresent) {
            specialtyRepository.delete(specialty.get())
            true
        } else {
            false
        }
    }
    
}