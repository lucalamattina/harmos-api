package ar.edu.itba.harmos.dtos.requests

data class EmailRequest(
    val to: String,
    val subject: String,
    val body: String
) 