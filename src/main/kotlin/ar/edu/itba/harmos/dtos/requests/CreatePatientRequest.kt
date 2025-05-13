package ar.edu.itba.harmos.dtos.requests

data class CreatePatientRequest (
    val name: String,
    val phone: String,
    val status: String,
    val userIds: List<Long> = emptyList() // Lista opcional de IDs de usuarios
)
//TODO: CHECK, tendria q haber mas? agregar doctores? status?