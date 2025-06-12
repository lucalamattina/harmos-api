package ar.edu.itba.harmos.models

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "announcement")
class Announcement(
    @Column(columnDefinition = "TEXT")
    var title: String,
    
    @Column(columnDefinition = "TEXT")
    var content: String,
    
    val date: LocalDateTime,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "announcement_specialty",
        joinColumns = [JoinColumn(name = "announcement_id")],
        inverseJoinColumns = [JoinColumn(name = "specialty_id")]
    )
    val specialties: MutableSet<Specialty>,

    @ManyToOne(fetch = FetchType.EAGER)
    val createdBy: AppUser,
    
    @ElementCollection
    @CollectionTable(name = "announcement_images", joinColumns = [JoinColumn(name = "announcement_id")])
    @Column(name = "image_url")
    var images: MutableList<String> = mutableListOf(),

    @ElementCollection
    @CollectionTable(name = "announcement_files", joinColumns = [JoinColumn(name = "announcement_id")])
    @Column(name = "file_url")
    var files: MutableList<String> = mutableListOf(),
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1
) {
    constructor() : this("","", LocalDateTime.MIN, mutableSetOf(), AppUser(), mutableListOf(), mutableListOf()) {

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Announcement

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}