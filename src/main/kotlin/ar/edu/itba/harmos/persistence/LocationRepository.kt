package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.Location
import org.springframework.data.repository.CrudRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface LocationRepository : CrudRepository<Location, Long> {
    fun findByName(name: String): Location?

    @Query("""
        SELECT l FROM Location l
        WHERE (:name IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%', :name, '%')))
    """)
    fun findLocations(@Param("name") name: String?): List<Location>
}
