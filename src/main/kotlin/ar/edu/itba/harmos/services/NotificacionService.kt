package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.models.Notificacion
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.persistence.NotificacionRepository
import org.springframework.stereotype.Service

@Service
class NotificacionService(
    private val notificacionRepository: NotificacionRepository
) {
    fun obtenerNoLeidas(usuario: AppUser): List<Notificacion> =
        notificacionRepository.findByUsuarioAndLeidaFalse(usuario)

    fun obtenerTodas(usuario: AppUser): List<Notificacion> =
        notificacionRepository.findByUsuario(usuario)

    fun marcarComoLeida(id: Long): Notificacion {
        val notificacion = notificacionRepository.findById(id).orElseThrow()
        val actualizada = Notificacion(
            mensaje = notificacion.mensaje,
            leida = true,
            fecha = notificacion.fecha,
            usuario = notificacion.usuario,
            id = notificacion.id
        )
        return notificacionRepository.save(actualizada)
    }

    fun crear(notificacion: Notificacion): Notificacion =
        notificacionRepository.save(notificacion)
} 