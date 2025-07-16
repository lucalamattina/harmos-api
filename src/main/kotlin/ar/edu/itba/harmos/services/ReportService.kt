package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateReportRequest
import ar.edu.itba.harmos.dtos.requests.EditReportRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.AppUserRole
import ar.edu.itba.harmos.models.Notification
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
    private val cloudinaryService: CloudinaryService,
    private val notificationService: NotificationService
) {
    
    fun createReportWithFile(createReportRequest: CreateReportRequest, doctor: AppUser, fileUrl: String): Report? {
        val patient = patientService.getPatientById(createReportRequest.patientId)
            ?: return null
        
        val specialty = specialtyService.getSpecialtyById(createReportRequest.specialtyId)
            ?: return null

        // Verificar permisos solo si NO es administrador
        if (!isAdmin(doctor)) {
            // Verificar que el doctor tenga acceso al paciente
            if (!patient.doctors.contains(doctor)) {
                throw IllegalAccessException("El doctor no tiene acceso a este paciente")
            }

            // Verificar que el doctor tenga la especialidad
            if (!doctor.specialties.contains(specialty)) {
                throw IllegalAccessException("El doctor no tiene esta especialidad")
            }
        }

        val report = Report(
            title = createReportRequest.title,
            patient = patient,
            doctor = doctor,
            specialty = specialty,
            fileUrl = fileUrl
        )

        val savedReport = reportRepository.save(report)

        // Notificar al dueño del reporte (el doctor que lo creó) solo si no es él mismo
        if (savedReport.doctor.id != doctor.id) {
            try {
                val creatorName = doctor.name
                val notification = Notification(
                    message = "Se ha creado un nuevo reporte por $creatorName para tu paciente ${savedReport.patient.name}: ${savedReport.title}",
                    user = savedReport.doctor,
                    reportId = savedReport.id
                )
                notificationService.create(notification)
            } catch (e: Exception) {
                println("Error creating notification for report ${savedReport.id}: ${e.message}")
                e.printStackTrace()
            }
        }

        return savedReport
    }

    fun updateReport(id: Long, editReportRequest: EditReportRequest, doctor: AppUser, newFile: org.springframework.web.multipart.MultipartFile? = null): Report? {
        val report = getReportById(id) ?: return null

        // Authorization is now handled in the controller
        // No need for redundant checks here

        // Resolver paciente si se proporciona un nuevo patientId
        val patient = if (editReportRequest.patientId != null) {
            val newPatient = patientService.getPatientById(editReportRequest.patientId)
                ?: throw IllegalArgumentException("Paciente no encontrado")
            
            // Verificar que el doctor tenga acceso al nuevo paciente (solo si no es admin)
            if (!isAdmin(doctor) && !newPatient.doctors.contains(doctor)) {
                throw IllegalAccessException("El doctor no tiene acceso a este paciente")
            }
            newPatient
        } else {
            report.patient
        }

        // Resolver especialidad si se proporciona un nuevo specialtyId
        val specialty = if (editReportRequest.specialtyId != null) {
            val newSpecialty = specialtyService.getSpecialtyById(editReportRequest.specialtyId)
                ?: throw IllegalArgumentException("Especialidad no encontrada")
            
            // Verificar que el doctor tenga la especialidad (solo si no es admin)
            if (!isAdmin(doctor) && !doctor.specialties.contains(newSpecialty)) {
                throw IllegalAccessException("El doctor no tiene esta especialidad")
            }
            newSpecialty
        } else {
            report.specialty
        }

        // Manejar actualización del archivo si se proporciona
        var newFileUrl = report.fileUrl
        if (newFile != null && !newFile.isEmpty) {
            try {
                // Subir el nuevo archivo
                val uploadedFileUrl = cloudinaryService.uploadDocument(newFile, "reports")
                if (uploadedFileUrl.isBlank()) {
                    throw RuntimeException("Error al subir el nuevo archivo")
                }
                
                // Borrar el archivo anterior si existe
                if (report.fileUrl.isNotBlank()) {
                    try {
                        println("Attempting to delete old report file: ${report.fileUrl}")
                        val deleted = cloudinaryService.deleteFileEnhanced(report.fileUrl, "raw")
                        if (!deleted) {
                            println("WARNING: Failed to delete old report file from Cloudinary: ${report.fileUrl}")
                        } else {
                            println("Successfully deleted old report file from Cloudinary")
                        }
                    } catch (e: Exception) {
                        println("Error eliminando archivo anterior ${report.fileUrl}: ${e.message}")
                        e.printStackTrace()
                    }
                }
                
                newFileUrl = uploadedFileUrl
            } catch (e: Exception) {
                println("Error actualizando archivo del reporte: ${e.message}")
                e.printStackTrace()
                throw RuntimeException("Error al actualizar el archivo del reporte: ${e.message}")
            }
        }

        // Crear un nuevo reporte con los valores actualizados
        val updatedReport = Report(
            title = editReportRequest.title ?: report.title,
            patient = patient,
            doctor = report.doctor,
            specialty = specialty,
            fileUrl = newFileUrl,
            date = report.date,
            id = report.id
        )

        val savedReport = reportRepository.save(updatedReport)

        // Notificar al dueño del reporte original (el doctor que lo creó) solo si no es él mismo
        if (savedReport.doctor.id != doctor.id) {
            try {
                val editorName = doctor.name
                val notification = Notification(
                    message = "Se ha modificado por $editorName tu reporte del paciente ${savedReport.patient.name}: ${savedReport.title}",
                    user = savedReport.doctor,
                    reportId = savedReport.id
                )
                notificationService.create(notification)
            } catch (e: Exception) {
                println("Error creating notification for report update ${savedReport.id}: ${e.message}")
                e.printStackTrace()
            }
        }

        return savedReport
    }

    fun deleteReport(id: Long, doctor: AppUser): Boolean {
        val report = getReportById(id) ?: return false

        // Authorization is now handled in the controller
        // No need for redundant checks here

        return try {
            // Eliminar archivo de Cloudinary usando método mejorado
            try {
                println("Attempting to delete report file: ${report.fileUrl}")
                val deleted = cloudinaryService.deleteFileEnhanced(report.fileUrl, "raw")
                if (!deleted) {
                    println("WARNING: Failed to delete report file from Cloudinary: ${report.fileUrl}")
                } else {
                    println("Successfully deleted report file from Cloudinary")
                }
            } catch (e: Exception) {
                println("Error eliminando archivo ${report.fileUrl}: ${e.message}")
                e.printStackTrace()
            }

            // Eliminar reporte de la base de datos
            reportRepository.deleteById(id)
            true
        } catch (e: Exception) {
            println("Error eliminando reporte: ${e.message}")
            e.printStackTrace()
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
    

    
    fun getReportsByDoctorId(doctorId: Long): List<Report> {
        return reportRepository.findByDoctorIdOrderByDateDesc(doctorId)
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

    private fun isAdmin(user: AppUser): Boolean {
        return user.roles.any { it.role == AppUserRole.ADMINISTRATOR.roleName }
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

} 