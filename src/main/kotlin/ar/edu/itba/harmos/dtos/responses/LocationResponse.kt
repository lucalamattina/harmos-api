package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.models.Location

data class LocationResponse(val id: Long, val name: String) {
    companion object {
        fun singleFromModel(location: Location): LocationResponse {
            return LocationResponse(location.id, location.name)
        }

        fun listFromModel(locations: List<Location>): List<LocationResponse> {
            return locations.map { singleFromModel(it) }
        }
    }
}
