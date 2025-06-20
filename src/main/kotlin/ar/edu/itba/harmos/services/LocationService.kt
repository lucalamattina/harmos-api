package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateLocationRequest
import ar.edu.itba.harmos.models.Location
import ar.edu.itba.harmos.persistence.LocationRepository
import org.springframework.stereotype.Service

@Service
class LocationService(private val locationRepository: LocationRepository) {

    fun createLocation(createLocationRequest: CreateLocationRequest): Location? {
        if (locationRepository.findByName(createLocationRequest.name.trim()) != null) {
            return null
        }
        val location = Location(createLocationRequest.name.trim())
        return locationRepository.save(location)
    }

    fun getLocationByName(name: String): Location? {
        return locationRepository.findByName(name.trim())
    }

    fun getLocationById(id: Long): Location? {
        val opt = locationRepository.findById(id)
        return if (opt.isPresent) {
            opt.get()
        } else {
            null
        }
    }

    fun getAllLocations(name: String? = null): List<Location> {
        return locationRepository.findLocations(name?.trim())
    }

    fun deleteLocationById(id: Long): Boolean {
        val location = locationRepository.findById(id)
        return if (location.isPresent) {
            locationRepository.delete(location.get())
            true
        } else {
            false
        }
    }
}
