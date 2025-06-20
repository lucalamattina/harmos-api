package ar.edu.itba.harmos.dtos.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class CreateLocationRequest(@JsonProperty("name") val name: String)
