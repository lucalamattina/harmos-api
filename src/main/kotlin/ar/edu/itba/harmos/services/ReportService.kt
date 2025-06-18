package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateReportRequest
import ar.edu.itba.harmos.dtos.requests.EditReportRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Report
import ar.edu.itba.harmos.persistence.ReportRepository
import org.springframework.stereotype.Service

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val patientService: PatientService,
    private val specialtyService: SpecialtyService,
    private val cloudinaryService: CloudinaryService
) {
    
    fun createReportWithFile(createReportRequest: CreateReportRequest, doctor: AppUser, fileUrl: String): Report? {
        val patient = patientService.getPatientById(createReportRequest.patientId)
            ?: return null
        
        val specialty = specialtyService.getSpecialtyById(createReportRequest.specialtyId)
            ?: return null

        // Verificar que el doctor tenga acceso al paciente
        if (!patient.doctors.contains(doctor)) {
            throw IllegalAccessException("El doctor no tiene acceso a este paciente")
        }

        // Verificar que el doctor tenga la especialidad
        if (!doctor.specialties.contains(specialty)) {
            throw IllegalAccessException("El doctor no tiene esta especialidad")
        }

        val report = Report(
            title = createReportRequest.title,
            patient = patient,
            doctor = doctor,
            specialty = specialty,
            fileUrl = fileUrl
        )

        return reportRepository.save(report)
    }

    fun updateReport(id: Long, editReportRequest: EditReportRequest, doctor: AppUser): Report? {
        val report = getReportById(id) ?: return null

        // Verificar que el doctor sea el creador del reporte
        if (report.doctor.id != doctor.id) {
            throw IllegalAccessException("Solo el doctor que creó el reporte puede editarlo")
        }

        // Crear un nuevo reporte con los valores actualizados
        val updatedReport = Report(
            title = editReportRequest.title ?: report.title,
            patient = report.patient,
            doctor = report.doctor,
            specialty = report.specialty,
            fileUrl = report.fileUrl,
            date = report.date,
            id = report.id
        )

        return reportRepository.save(updatedReport)
    }

    fun deleteReport(id: Long, doctor: AppUser): Boolean {
        val report = getReportById(id) ?: return false

        // Verificar que el doctor sea el creador del reporte
        if (report.doctor.id != doctor.id) {
            throw IllegalAccessException("Solo el doctor que creó el reporte puede eliminarlo")
        }

        return try {
            // Eliminar archivo de Cloudinary
            try {
                val publicId = cloudinaryService.extractPublicId(report.fileUrl)
                cloudinaryService.deleteFile(publicId, "raw")
            } catch (e: Exception) {
                println("Error eliminando archivo ${report.fileUrl}: ${e.message}")
            }

            // Eliminar reporte de la base de datos
            reportRepository.deleteById(id)
            true
        } catch (e: Exception) {
            println("Error eliminando reporte: ${e.message}")
            false
        }
    }
    
    fun getReportById(id: Long): Report? {
        val opt = reportRepository.findById(id)
        return if (opt.isPresent) {
            opt.get()
        } else {
            null
        }
    }
    
    fun getAllReports(): List<Report> {
        return reportRepository.findAll().toList()
    }
    
    fun getReportsByPatientId(patientId: Long, specialtyId: Long? = null): List<Report> {
        return if (specialtyId != null) {
            reportRepository.findByPatientIdAndSpecialtyIdOrderByDateDesc(patientId, specialtyId)
        } else {
            reportRepository.findByPatientIdOrderByDateDesc(patientId)
        }
    }
    
    fun getReportsByDoctorId(doctorId: Long): List<Report> {
        return reportRepository.findByDoctorIdOrderByDateDesc(doctorId)
    }

    fun getReportsBySpecialtyId(specialtyId: Long): List<Report> {
        return reportRepository.findBySpecialtyId(specialtyId)
    }

    fun getReportsForDoctor(doctor: AppUser, patientId: Long? = null, specialtyId: Long? = null): List<Report> {
        return when {
            patientId != null && specialtyId != null -> {
                val reports = reportRepository.findByPatientIdAndSpecialtyIdOrderByDateDesc(patientId, specialtyId)
                reports.filter { canDoctorAccessReport(doctor, it) }
            }
            patientId != null -> {
                val reports = reportRepository.findByPatientIdOrderByDateDesc(patientId)
                reports.filter { canDoctorAccessReport(doctor, it) }
            }
            specialtyId != null -> {
                val reports = reportRepository.findBySpecialtyId(specialtyId)
                reports.filter { canDoctorAccessReport(doctor, it) }
            }
            else -> getReportsByDoctorId(doctor.id)
        }
    }

    private fun canDoctorAccessReport(doctor: AppUser, report: Report): Boolean {
        // El doctor puede acceder si es el creador o si tiene acceso al paciente
        return report.doctor.id == doctor.id || report.patient.doctors.contains(doctor)
    }
} 