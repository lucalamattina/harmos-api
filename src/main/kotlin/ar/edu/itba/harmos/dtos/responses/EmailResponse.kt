package ar.edu.itba.harmos.dtos.responses

data class EmailResponse(
    val id: String = java.util.UUID.randomUUID().toString(),
    val status: String = "sent",
    val recipient: String,
    val subject: String
) 