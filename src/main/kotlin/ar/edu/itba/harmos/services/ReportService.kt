package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateReportRequest
import ar.edu.itba.harmos.dtos.requests.EditReportRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Report
import ar.edu.itba.harmos.persistence.ReportRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

        // Authorization is now handled in the controller
        // No need for redundant checks here

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

        // Authorization is now handled in the controller
        // No need for redundant checks here

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

    // ========================= PAGINATED METHODS =========================

    /**
     * Get all reports with pagination and optional filters (for admins)
     */
    fun getAllReportsPaginated(
        patientId: Long? = null,
        specialtyId: Long? = null,
        doctorId: Long? = null,
        title: String? = null,
        pageable: Pageable
    ): Page<Report> {
        val titleFilter = if (title.isNullOrBlank()) null else title.trim()
        
        return when {
            // All filters
            titleFilter != null && patientId != null && specialtyId != null && doctorId != null -> {
                reportRepository.findByTitleContainingIgnoreCaseAndPatientIdAndSpecialtyIdAndDoctorIdOrderByDateDesc(titleFilter, patientId, specialtyId, doctorId, pageable)
            }
            // Title + 2 other filters
            titleFilter != null && patientId != null && specialtyId != null -> {
                reportRepository.findByTitleContainingIgnoreCaseAndPatientIdAndSpecialtyIdOrderByDateDesc(titleFilter, patientId, specialtyId, pageable)
            }
            titleFilter != null && patientId != null && doctorId != null -> {
                reportRepository.findByTitleContainingIgnoreCaseAndPatientIdAndDoctorIdOrderByDateDesc(titleFilter, patientId, doctorId, pageable)
            }
            titleFilter != null && specialtyId != null && doctorId != null -> {
                reportRepository.findByTitleContainingIgnoreCaseAndSpecialtyIdAndDoctorIdOrderByDateDesc(titleFilter, specialtyId, doctorId, pageable)
            }
            // 3 filters without title
            patientId != null && specialtyId != null && doctorId != null -> {
                reportRepository.findByPatientIdAndSpecialtyIdAndDoctorIdOrderByDateDesc(patientId, specialtyId, doctorId, pageable)
            }
            // Title + 1 other filter
            titleFilter != null && patientId != null -> {
                reportRepository.findByTitleContainingIgnoreCaseAndPatientIdOrderByDateDesc(titleFilter, patientId, pageable)
            }
            titleFilter != null && specialtyId != null -> {
                reportRepository.findByTitleContainingIgnoreCaseAndSpecialtyIdOrderByDateDesc(titleFilter, specialtyId, pageable)
            }
            titleFilter != null && doctorId != null -> {
                reportRepository.findByTitleContainingIgnoreCaseAndDoctorIdOrderByDateDesc(titleFilter, doctorId, pageable)
            }
            // 2 filters without title
            patientId != null && specialtyId != null -> {
                reportRepository.findByPatientIdAndSpecialtyIdOrderByDateDesc(patientId, specialtyId, pageable)
            }
            patientId != null && doctorId != null -> {
                reportRepository.findByPatientIdAndDoctorIdOrderByDateDesc(patientId, doctorId, pageable)
            }
            specialtyId != null && doctorId != null -> {
                reportRepository.findBySpecialtyIdAndDoctorIdOrderByDateDesc(specialtyId, doctorId, pageable)
            }
            // Single filters
            titleFilter != null -> {
                reportRepository.findByTitleContainingIgnoreCaseOrderByDateDesc(titleFilter, pageable)
            }
            patientId != null -> {
                reportRepository.findByPatientIdOrderByDateDesc(patientId, pageable)
            }
            specialtyId != null -> {
                reportRepository.findBySpecialtyIdOrderByDateDesc(specialtyId, pageable)
            }
            doctorId != null -> {
                reportRepository.findByDoctorIdOrderByDateDesc(doctorId, pageable)
            }
            // No filters
            else -> {
                reportRepository.findAllByOrderByDateDesc(pageable)
            }
        }
    }

    /**
     * Get reports for a doctor with pagination and optional filters
     */
    fun getReportsForDoctorPaginated(
        doctor: AppUser, 
        patientId: Long? = null, 
        specialtyId: Long? = null,
        doctorId: Long? = null,
        title: String? = null,
        pageable: Pageable
    ): Page<Report> {
        val titleFilter = if (title.isNullOrBlank()) null else title.trim()
        
        // If doctorId is not specified, default to the current doctor's reports
        val effectiveDoctorId = doctorId ?: doctor.id
        
        return when {
            // All filters
            titleFilter != null && patientId != null && specialtyId != null -> {
                reportRepository.findByTitleContainingIgnoreCaseAndPatientIdAndSpecialtyIdAndDoctorIdOrderByDateDesc(titleFilter, patientId, specialtyId, effectiveDoctorId, pageable)
            }
            // Title + 1 other filter + doctorId
            titleFilter != null && patientId != null -> {
                reportRepository.findByTitleContainingIgnoreCaseAndPatientIdAndDoctorIdOrderByDateDesc(titleFilter, patientId, effectiveDoctorId, pageable)
            }
            titleFilter != null && specialtyId != null -> {
                reportRepository.findByTitleContainingIgnoreCaseAndSpecialtyIdAndDoctorIdOrderByDateDesc(titleFilter, specialtyId, effectiveDoctorId, pageable)
            }
            // 2 filters + doctorId (without title)
            patientId != null && specialtyId != null -> {
                reportRepository.findByPatientIdAndSpecialtyIdAndDoctorIdOrderByDateDesc(patientId, specialtyId, effectiveDoctorId, pageable)
            }
            // Title + doctorId only
            titleFilter != null -> {
                reportRepository.findByTitleContainingIgnoreCaseAndDoctorIdOrderByDateDesc(titleFilter, effectiveDoctorId, pageable)
            }
            // Single filter + doctorId
            patientId != null -> {
                reportRepository.findByPatientIdAndDoctorIdOrderByDateDesc(patientId, effectiveDoctorId, pageable)
            }
            specialtyId != null -> {
                reportRepository.findBySpecialtyIdAndDoctorIdOrderByDateDesc(specialtyId, effectiveDoctorId, pageable)
            }
            // Only doctorId
            else -> {
                reportRepository.findByDoctorIdOrderByDateDesc(effectiveDoctorId, pageable)
            }
        }
    }

    /**
     * Get reports by patient ID with pagination
     */
    fun getReportsByPatientIdPaginated(patientId: Long, specialtyId: Long? = null, pageable: Pageable): Page<Report> {
        return if (specialtyId != null) {
            reportRepository.findByPatientIdAndSpecialtyIdOrderByDateDesc(patientId, specialtyId, pageable)
        } else {
            reportRepository.findByPatientIdOrderByDateDesc(patientId, pageable)
        }
    }

    /**
     * Get reports by doctor ID with pagination
     */
    fun getReportsByDoctorIdPaginated(doctorId: Long, pageable: Pageable): Page<Report> {
        return reportRepository.findByDoctorIdOrderByDateDesc(doctorId, pageable)
    }

    /**
     * Get reports by specialty ID with pagination
     */
    fun getReportsBySpecialtyIdPaginated(specialtyId: Long, pageable: Pageable): Page<Report> {
        return reportRepository.findBySpecialtyIdOrderByDateDesc(specialtyId, pageable)
    }
} 