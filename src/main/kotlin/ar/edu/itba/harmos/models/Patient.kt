package ar.edu.itba.harmos.models

import javax.persistence.*

@Entity
@Table(name = "Patients")
class Patient(
    val name: String,
    val phone: String,
    val status: String,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "patient_doctor",
        joinColumns = [JoinColumn(name = "patient_id")],
        inverseJoinColumns = [JoinColumn(name = "doctor_id")]
    )
    val doctors: MutableList<AppUser>,

    @OneToMany(mappedBy = "patient", cascade = [CascadeType.ALL], orphanRemoval = true)
    val reports: List<Report>,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1
) {
    constructor() : this("", "", "", mutableListOf(), emptyList())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Patient

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
