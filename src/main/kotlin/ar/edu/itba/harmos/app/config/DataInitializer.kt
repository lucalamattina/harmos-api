package ar.edu.itba.harmos.app.config

import ar.edu.itba.harmos.models.*
import ar.edu.itba.harmos.persistence.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.DayOfWeek
import kotlin.random.Random

@Configuration
class DataInitializer(
    private val appUserRepository: AppUserRepository,
    private val roleRepository: RoleRepository,
    private val specialtyRepository: SpecialtyRepository,
    private val patientRepository: PatientRepository,
    private val scheduleRepository: ScheduleRepository,
    private val announcementRepository: AnnouncementRepository,
    private val notificationRepository: NotificationRepository,
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
                initializeUsers()
                initializePatients()
                initializeSchedules()
                initializeAnnouncements()
                initializeNotifications()
            }
        }
    }

    private fun clearDatabase() {
        // Clear tables in reverse order of dependencies
        notificationRepository.deleteAll()
        announcementRepository.deleteAll()
        scheduleRepository.deleteAll()
        patientRepository.deleteAll()
        appUserRepository.deleteAll()
        specialtyRepository.deleteAll()
        roleRepository.deleteAll()
    }

    private fun initializeRoles() {
        val roles = listOf(
            Role(role = AppUserRole.DOCTOR.roleName),
            Role(role = AppUserRole.ADMINISTRATOR.roleName)
        )
        roleRepository.saveAll(roles)
    }

    private fun initializeSpecialties() {
        val specialties = listOf(
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
        val doctors = (1..30).map { i ->
            AppUser(
                email = "doctor$i@example.com",
                password = passwordEncoder.encode("password"),
                firstName = "Doctor",
                lastName = "Number $i",
                phone = "1234567890",
                specialties = mutableSetOf(allSpecialties.elementAt(Random.nextInt(allSpecialties.size))),
                roles = mutableSetOf(doctorRole)
            )
        }

        // Create admin user
        val admin = AppUser(
            email = "noreplyharmos@gmail.com",
            password = passwordEncoder.encode("password"),
            firstName = "SUPER",
            lastName = "USER",
            phone = "3453453456",
            specialties = allSpecialties,
            roles = mutableSetOf(adminRole)
        )

        appUserRepository.saveAll(doctors + admin)
    }

    private fun initializePatients() {
        val patients = (1..50).map { i ->
            Patient(
                name = "Patient $i",
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
        val doctors = appUserRepository.findAll()
        val schedules = doctors.take(5).mapIndexed { index, doctor ->
            Schedule(
                location = "Consultorio ${index + 1}",
                dayOfWeek = DayOfWeek.values()[index],
                hourFrom = 9,
                minuteFrom = 0,
                hourTo = 17,
                minuteTo = 0,
                doctor = doctor
            )
        }
        scheduleRepository.saveAll(schedules)
    }

    private fun initializeAnnouncements() {
        val doctors = appUserRepository.findAll().toList()
        val specialties = specialtyRepository.findAll().toList()
        
        val announcements = (1..25).map { i ->
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
        
        // Create some general notifications for all users
        users.forEach { user ->
            notifications.add(
                Notification(
                    message = "Welcome to Harmos! Your account has been created successfully.",
                    read = Random.nextBoolean(),
                    date = LocalDateTime.now().minusDays(Random.nextLong(1, 7)),
                    user = user,
                    announcementId = null
                )
            )
            
            notifications.add(
                Notification(
                    message = "System maintenance scheduled for this weekend. Please save your work.",
                    read = Random.nextBoolean(),
                    date = LocalDateTime.now().minusDays(Random.nextLong(0, 3)),
                    user = user,
                    announcementId = null
                )
            )
        }
        
        // Create notifications related to announcements
        // Simplified approach: create notifications for a subset of users for each announcement
        announcements.forEach { announcement ->
            // Create notifications for random users (simulating specialty-based targeting)
            val randomUsers = users.shuffled().take(Random.nextInt(5, 15))
            
            randomUsers.forEach { user ->


                // Don't create notification for the announcement creator
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