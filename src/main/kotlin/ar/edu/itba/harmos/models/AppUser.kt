package ar.edu.itba.harmos.models

import javax.persistence.*

@Entity
@Table(name = "users")
class AppUser (
    val email: String,
    var password: String,
    val firstName: String,
    val lastName: String,
    val phone: String,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_specialty",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "specialty_id")]
    )
    val specialties: MutableSet<Specialty>,

    @ManyToMany(fetch = FetchType.EAGER)
    val roles: Set<Role>,

    @OneToMany(mappedBy = "createdBy", cascade = [CascadeType.REMOVE], orphanRemoval = true)
    val announcements: List<Announcement> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1
) {
    val name: String
        get() = "$firstName $lastName"

    constructor() : this("","","","","", mutableSetOf(), emptySet())

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