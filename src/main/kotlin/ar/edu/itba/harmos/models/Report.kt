package ar.edu.itba.harmos.models

import javax.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "Reports")
class Report(
    val title: String,

    @ManyToOne
    @JoinColumn(name = "patient_id")
    val patient: Patient,

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    val doctor: AppUser,

    @ManyToOne
    @JoinColumn(name = "specialty_id")
    val specialty: Specialty,

    @Column(name = "file_url")
    val fileUrl: String,

    val date: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1,
){
    constructor() : this( "", Patient(), AppUser(), Specialty(), "", LocalDateTime.now())

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
