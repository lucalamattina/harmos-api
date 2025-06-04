package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.models.Notificacion
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.services.NotificacionService
import ar.edu.itba.harmos.security.annotations.CurrentUser
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ar.edu.itba.harmos.dtos.responses.NotificacionResponse

@RestController
@RequestMapping("/notificaciones")
class NotificacionController(
    private val notificacionService: NotificacionService
) {
    @GetMapping
    fun getNotificaciones(
        @CurrentUser usuario: AppUser?,
        @RequestParam(required = false, defaultValue = "false") soloNoLeidas: Boolean
    ): ResponseEntity<List<NotificacionResponse>> {
        if (usuario == null) return ResponseEntity(HttpStatus.UNAUTHORIZED)
        val notificaciones = if (soloNoLeidas)
            notificacionService.obtenerNoLeidas(usuario)
        else
            notificacionService.obtenerTodas(usuario)
        val response = notificaciones.map { NotificacionResponse(it.id, it.mensaje, it.leida, it.fecha, it.anuncioId) }
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{id}/leida")
    fun marcarComoLeida(@PathVariable id: Long): ResponseEntity<NotificacionResponse> {
        val notificacion = notificacionService.marcarComoLeida(id)
        val response = NotificacionResponse(notificacion.id, notificacion.mensaje, notificacion.leida, notificacion.fecha, notificacion.anuncioId)
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun crearNotificacion(@RequestBody notificacion: Notificacion): ResponseEntity<NotificacionResponse> {
        val creada = notificacionService.crear(notificacion)
        val response = NotificacionResponse(creada.id, creada.mensaje, creada.leida, creada.fecha, creada.anuncioId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
} 