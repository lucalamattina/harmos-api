package ar.edu.itba.harmos.models

import javax.persistence.*

@Entity
@Table(name = "users")
class AppUser (
    val email: String,
    var password: String,
    var firstName: String,
    var lastName: String,
    var phone: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id")
    var specialty: Specialty? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    val roles: MutableSet<Role> = mutableSetOf(),

    @OneToMany(mappedBy = "createdBy", cascade = [CascadeType.REMOVE], orphanRemoval = true)
    val announcements: List<Announcement> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1
) {
    val name: String
        get() = "$firstName $lastName"

    constructor() : this("", "", "", "", "")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppUser

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}