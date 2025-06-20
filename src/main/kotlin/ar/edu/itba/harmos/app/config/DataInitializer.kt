package ar.edu.itba.harmos.app.config

import ar.edu.itba.harmos.models.*
import ar.edu.itba.harmos.persistence.*
import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlin.random.Random
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional

@Configuration
class DataInitializer(
        private val appUserRepository: AppUserRepository,
        private val roleRepository: RoleRepository,
        private val specialtyRepository: SpecialtyRepository,
        private val patientRepository: PatientRepository,
        private val scheduleRepository: ScheduleRepository,
        private val locationRepository: LocationRepository,
        private val announcementRepository: AnnouncementRepository,
        private val notificationRepository: NotificationRepository,
        private val passwordResetTokenRepository: PasswordResetTokenRepository,
        private val reportRepository: ReportRepository,
        private val passwordEncoder: PasswordEncoder,
        @Value("\${app.database.repopulate:false}") private val repopulateDatabase: Boolean
) {

    @Bean
    @Transactional
    fun initData(): CommandLineRunner {
        return CommandLineRunner {
            if (repopulateDatabase) {
                clearDatabase()
                initializeRoles()
                initializeSpecialties()
                initializeUsers() // Initialize users before patients and schedules
                initializeLocations()
                initializePatients()
                initializeSchedules()
                initializeAnnouncements()
                initializeNotifications()
            }
        }
    }

    private fun clearDatabase() {
        // Delete in an order that respects foreign key constraints
        passwordResetTokenRepository.deleteAll() // Depends on AppUser
        reportRepository.deleteAll()             // Depends on Patient, AppUser, Specialty
        notificationRepository.deleteAll()       // Depends on AppUser, Announcement
        scheduleRepository.deleteAll()         // Depends on Location, AppUser, Patient
        announcementRepository.deleteAll()     // Depends on AppUser; M2M Specialty
        patientRepository.deleteAll()          // M2M AppUser
        appUserRepository.deleteAll()          // M2M Specialty, M2M Role
        locationRepository.deleteAll()         // Base entity
        specialtyRepository.deleteAll()        // Base entity
        roleRepository.deleteAll()             // Base entity
    }

    private fun initializeRoles() {
        val roles =
                listOf(
                        Role(role = AppUserRole.DOCTOR.roleName),
                        Role(role = AppUserRole.ADMINISTRATOR.roleName)
                )
        roleRepository.saveAll(roles)
    }

    private val firstNames = listOf(
        "Sofia", "Mateo", "Valentina", "Santiago", "Isabella", "Benjamin", "Camila", "Thiago",
        "Emma", "Lucas", "Martina", "Joaquin", "Mia", "Bautista", "Olivia", "Agustin",
        "Catalina", "Facundo", "Elena", "Tomas", "Abril", "Ignacio", "Julieta", "Nicolas",
        "Renata", "Lautaro", "Zoe", "Francisco", "Alma", "Juan"
    )

    private val lastNames = listOf(
        "Gonzalez", "Rodriguez", "Gomez", "Fernandez", "Lopez", "Diaz", "Martinez", "Perez",
        "Garcia", "Sanchez", "Romero", "Sosa", "Torres", "Alvarez", "Ruiz", "Ramirez",
        "Flores", "Benitez", "Acosta", "Medina", "Herrera", "Suarez", "Aguirre", "Gimenez",
        "Molina", "Castro", "Ortiz", "Silva", "Nuñez", "Luna"
    )

    private fun getRandomFirstName(): String {
        return firstNames.random()
    }

    private fun getRandomLastName(): String {
        return lastNames.random()
    }


    private fun initializeSpecialties() {
        val specialties =
                listOf(
                        Specialty(name = "TO"),
                        Specialty(name = "FONO"),
                        Specialty(name = "KINE"),
                        Specialty(name = "PSICO"),
                        Specialty(name = "FISIO")
                )
        specialtyRepository.saveAll(specialties)
    }

    private fun initializeUsers() {
        val doctorRole = roleRepository.findByRole(AppUserRole.DOCTOR.roleName)!!
        val adminRole = roleRepository.findByRole(AppUserRole.ADMINISTRATOR.roleName)!!
        val allSpecialties = specialtyRepository.findAll().toMutableSet()

        // Create 30 doctors
        val doctors =
                (1..30).map { i ->
                    AppUser(
                            email = "${getRandomFirstName().lowercase()}.${getRandomLastName().lowercase()}$i@example.com",
                            password = passwordEncoder.encode("password"),
                            firstName = getRandomFirstName(),
                            lastName = getRandomLastName(),
                            phone = "1234567890",
                            specialties =
                                    mutableSetOf(
                                            allSpecialties.elementAt(
                                                    Random.nextInt(allSpecialties.size)
                                            )
                                    ),
                            roles = mutableSetOf(doctorRole)
                    )
                }

        // Create admin user
        val admin =
                AppUser(
                        email = "noreplyharmos@gmail.com",
                        password = passwordEncoder.encode("password"),
                        firstName = "SUPER",
                        lastName = "USER",
                        phone = "3453453456",
                        specialties = mutableSetOf(),
                        roles = mutableSetOf(adminRole)
                )

        appUserRepository.saveAll(doctors + admin)
    }

    private fun initializeLocations() {
        val locations =
                listOf(
                        Location(name = "Consultorio 1"),
                        Location(name = "Consultorio 2"),
                        Location(name = "Consultorio 3"),
                        Location(name = "Sala de Terapia Grupal"),
                        Location(name = "Oficina Administrativa")
                )
        locationRepository.saveAll(locations)
    }

    private fun initializePatients() {
        val patients =
                (1..50).map { i ->
                    Patient(
                            name = "${getRandomFirstName()} ${getRandomLastName()}",
                            phone = "1234567890",
                            status = PatientStatus.ACTIVE,
                            doctors = mutableListOf(),
                            reports = emptyList()
                    )
                }
        patientRepository.saveAll(patients)

        // Assign doctors to patients
        val allDoctors = appUserRepository.findAll()
        val allPatients = patientRepository.findAll()

        allPatients.forEach { patient ->
            val randomDoctors = allDoctors.shuffled().take((1..3).random())
            patient.doctors.addAll(randomDoctors)
        }
        patientRepository.saveAll(allPatients)
    }

    private fun initializeSchedules() {
        val doctors = appUserRepository.findAll().toList()
        val patients = patientRepository.findAll().toList()
        val locations = locationRepository.findAll().toList()
        val daysOfWeek = DayOfWeek.values()

        if (doctors.isEmpty() || patients.isEmpty() || locations.isEmpty()) {
            println("Not enough doctors, patients, or locations to create schedules.")
            return
        }

        val schedulesToCreate = mutableListOf<Schedule>()
        val numberOfSchedules = 10 // Create 10 schedules

        for (i in 0 until numberOfSchedules) {
            val randomDoctor = doctors.random()
            val randomPatient = patients.random()
            val randomLocation = locations.random()
            val randomDay = daysOfWeek.random()
            val hourFrom = Random.nextInt(9, 17)
            val minuteFrom = listOf(0, 15, 30, 45).random()
            val hourTo = hourFrom + 1
            val minuteTo = minuteFrom

            Schedule(
                            location = randomLocation,
                            dayOfWeek = randomDay,
                            hourFrom = hourFrom,
                            minuteFrom = minuteFrom,
                            hourTo = hourTo,
                            minuteTo = minuteTo,
                            doctor = randomDoctor,
                            patient = randomPatient
                    )
                    .also { schedulesToCreate.add(it) }
        }

        scheduleRepository.saveAll(schedulesToCreate)
    }

    private fun initializeAnnouncements() {
        val doctors = appUserRepository.findAll().toList()
        val specialties = specialtyRepository.findAll().toList()

        val announcements =
                (1..25).map { i ->
                    val doctor = doctors[Random.nextInt(doctors.size)]
                    val specialty = specialties[Random.nextInt(specialties.size)]
                    Announcement(
                            title = "Announcement $i",
                            content = "Content for announcement $i",
                            date = LocalDateTime.now(),
                            specialties = mutableSetOf(specialty),
                            createdBy = doctor
                    )
                }
        announcementRepository.saveAll(announcements)
    }

    private fun initializeNotifications() {
        val users = appUserRepository.findAll().toList()
        val announcements = announcementRepository.findAll().toList()

        val notifications = mutableListOf<Notification>()

        users.forEach { user ->
            notifications.add(
                    Notification(
                            message =
                                    "Welcome to Harmos! Your account has been created successfully.",
                            read = Random.nextBoolean(),
                            date = LocalDateTime.now().minusDays(Random.nextLong(1, 7)),
                            user = user,
                            announcementId = null
                    )
            )

            notifications.add(
                    Notification(
                            message =
                                    "System maintenance scheduled for this weekend. Please save your work.",
                            read = Random.nextBoolean(),
                            date = LocalDateTime.now().minusDays(Random.nextLong(0, 3)),
                            user = user,
                            announcementId = null
                    )
            )
        }

        announcements.forEach { announcement ->
            val randomUsers = users.shuffled().take(Random.nextInt(5, 15))
            randomUsers.forEach { user ->
                if (user.id != announcement.createdBy.id) {
                    notifications.add(
                            Notification(
                                    message = "New announcement: ${announcement.title}",
                                    read = Random.nextBoolean(),
                                    date = announcement.date.plusMinutes(Random.nextLong(1, 30)),
                                    user = user,
                                    announcementId = announcement.id
                            )
                    )
                }
            }
        }
    }
}
