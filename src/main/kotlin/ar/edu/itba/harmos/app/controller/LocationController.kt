package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreateLocationRequest
import ar.edu.itba.harmos.dtos.responses.LocationResponse
import ar.edu.itba.harmos.services.LocationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/locations")
class LocationController(private val locationService: LocationService) {

    @PostMapping()
    @ResponseBody
    fun create(@RequestBody createLocationRequest: CreateLocationRequest): ResponseEntity<Any> {
        val location = locationService.createLocation(createLocationRequest)
        return if (location != null) {
            ResponseEntity(LocationResponse.singleFromModel(location), HttpStatus.CREATED)
        } else ResponseEntity(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/{id}")
    @ResponseBody
    fun getById(@PathVariable id: Long): ResponseEntity<Any> {
        val location = locationService.getLocationById(id)
        return if (location != null) {
            ResponseEntity(LocationResponse.singleFromModel(location), HttpStatus.OK)
        } else ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @GetMapping
    @ResponseBody
    fun getAll(@RequestParam(required = false) name: String?): ResponseEntity<List<LocationResponse>> {
        val locations = locationService.getAllLocations(name)
        return ResponseEntity.ok(LocationResponse.listFromModel(locations))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Any> {
        return if (locationService.deleteLocationById(id)) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
}
