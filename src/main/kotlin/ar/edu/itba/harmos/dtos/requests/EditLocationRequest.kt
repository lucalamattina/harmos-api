package ar.edu.itba.harmos.dtos.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class EditLocationRequest(@JsonProperty("name") val name: String)
