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
    @CollectionTable(name = "report_images", joinColumns = [JoinColumn(name = "report_id")])
    @Column(name = "image_url")
    var images: MutableList<String> = mutableListOf(),

    @ElementCollection
    @CollectionTable(name = "report_files", joinColumns = [JoinColumn(name = "report_id")])
    @Column(name = "file_url")
    var files: MutableList<String> = mutableListOf(),

    val date: LocalDateTime,

    @OneToMany(mappedBy = "report", cascade = [CascadeType.ALL], orphanRemoval = true)
    val comments: List<Comment>,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1,
){
    constructor() : this( "", Patient(), AppUser(), mutableListOf(), mutableListOf(), LocalDateTime.now(),  emptyList())

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
