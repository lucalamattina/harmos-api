package ar.edu.itba.harmos.models

import javax.persistence.*

@Entity
@Table(name = "specialties")
class Specialty(
    @Column(nullable = false, unique = true)
    val name: String,

    @ManyToMany(mappedBy = "specialties", fetch = FetchType.LAZY)
    val users: MutableSet<AppUser>,

    @ManyToMany(mappedBy = "specialties", fetch = FetchType.LAZY)
    val announcements: MutableSet<Announcement>,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1
) {
    constructor() : this("", mutableSetOf(), mutableSetOf())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Specialty

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}



