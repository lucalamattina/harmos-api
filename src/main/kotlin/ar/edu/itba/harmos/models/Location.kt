package ar.edu.itba.harmos.models

import javax.persistence.*

@Entity
@Table(name = "locations")
class Location(
        @Column(nullable = false, unique = true) val name: String, // Ensure name is not nullable and unique
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = -1
) {
    constructor() : this("")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Location

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
