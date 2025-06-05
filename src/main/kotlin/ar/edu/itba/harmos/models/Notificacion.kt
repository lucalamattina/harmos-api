package ar.edu.itba.harmos.models

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "notificacion")
class Notificacion(
    @Column(nullable = false)
    val mensaje: String,

    @Column(nullable = false)
    val leida: Boolean = false,

    @Column(nullable = false)
    val fecha: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    val usuario: AppUser,

    @Column(nullable = true)
    val anuncioId: Long? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1
) {
    constructor() : this("", false, LocalDateTime.now(), AppUser(), null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Notificacion
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
} 