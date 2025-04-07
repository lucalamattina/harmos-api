package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.Specialty
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Repository
interface SpecialtyRepository : CrudRepository<Specialty, Long> {
    fun findByName(name: String): Specialty?

    fun findAll(pageable: Pageable): Page<Specialty>

    override fun findAll(): List<Specialty>


}