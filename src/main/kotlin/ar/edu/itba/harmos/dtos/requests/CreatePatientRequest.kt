package ar.edu.itba.harmos.dtos.requests

data class CreatePatientRequest (
    val name: String,
    val phone: String,
    val status: String,
)
//TODO: CHECK, tendria q haber mas? agregar doctores? status?