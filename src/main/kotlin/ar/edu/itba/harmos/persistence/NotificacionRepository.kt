package ar.edu.itba.harmos.persistence

import ar.edu.itba.harmos.models.Notificacion
import ar.edu.itba.harmos.models.AppUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificacionRepository : JpaRepository<Notificacion, Long> {
    fun findByUsuarioAndLeidaFalse(usuario: AppUser): List<Notificacion>
    fun findByUsuario(usuario: AppUser): List<Notificacion>
} 