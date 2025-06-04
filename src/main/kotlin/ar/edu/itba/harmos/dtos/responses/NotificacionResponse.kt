package ar.edu.itba.harmos.dtos.responses

import java.time.LocalDateTime

data class NotificacionResponse(
    val id: Long,
    val mensaje: String,
    val leida: Boolean,
    val fecha: LocalDateTime,
    val anuncioId: Long?
) 