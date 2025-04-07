package ar.edu.itba.harmos.models

import javax.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "Comments")
class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1,

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    val createdBy: AppUser,

    @ManyToOne
    @JoinColumn(name = "report_id")
    val report: Report,

    val date: LocalDateTime,

    val message: String
) {
    // Protected no-arg constructor required by JPA
    protected constructor() : this(-1, AppUser(), Report(), LocalDateTime.now(), "")
}

