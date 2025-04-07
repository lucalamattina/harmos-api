package ar.edu.itba.harmos.models

import javax.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "Reports")
class Report(
    val status: String,

    @ManyToOne
    @JoinColumn(name = "patient_id")
    val patient: Patient,

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    val doctor: AppUser,

    @ElementCollection
    val files: List<String>,

    val date: LocalDateTime,

    @OneToMany(mappedBy = "report", cascade = [CascadeType.ALL], orphanRemoval = true)
    val comments: List<Comment>,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1,
){
    constructor() : this( "", Patient(), AppUser(), emptyList(), LocalDateTime.now(),  emptyList())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Report

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
