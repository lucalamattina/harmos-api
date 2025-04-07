package ar.edu.itba.harmos.dtos.requests

data class CreateSpecialtyRequest (
    val name: String,
    val data: String? = null
)