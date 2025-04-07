package ar.edu.itba.harmos.models

import java.time.DayOfWeek
import javax.persistence.*

@Entity
@Table(name = "schedule")
class Schedule(
    val location: String,
    val dayOfWeek: DayOfWeek,
    val hourFrom: Int,
    val minuteFrom: Int,
    val hourTo: Int,
    val minuteTo: Int,
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    val doctor: AppUser,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1
) {
    constructor() : this("", DayOfWeek.MONDAY, 0, 0, 0, 0,  AppUser())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Schedule

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}