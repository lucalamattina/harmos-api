package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.dtos.requests.CreateReportRequest
import ar.edu.itba.harmos.dtos.requests.EditReportRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.AppUserRole
import ar.edu.itba.harmos.models.Notification
import ar.edu.itba.harmos.models.Report
import ar.edu.itba.harmos.persistence.ReportRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val patientService: PatientService,
    private val specialtyService: SpecialtyService,
    private val cloudinaryService: CloudinaryService,
    private val notificationService: NotificationService,
    private val asyncNotificationService: AsyncNotificationService
) {

    private val logger = LoggerFactory.getLogger(ReportService::class.java)

    // ========================= SHARED HELPERS =========================

    fun isAdmin(user: AppUser): Boolean {
        return user.roles.any { it.role == AppUserRole.ADMINISTRATOR.roleName }
    }

    // ========================= SPECIFICATIONS =========================

    private val allowedSortFields = setOf("id", "title", "date")

    private fun byTitle(title: String?) = Specification<Report> { root, _, cb ->
        if (title != null) cb.like(cb.lower(root.get("title")), "%${title.lowercase()}%") else null
    }

    private fun byPatientId(patientId: Long?) = Specification<Report> { root, _, cb ->
        if (patientId != null) cb.equal(root.get<Any>("patient").get<Long>("id"), patientId) else null
    }

    private fun bySpecialtyId(specialtyId: Long?) = Specification<Report> { root, _, cb ->
        if (specialtyId != null) cb.equal(root.get<Any>("specialty").get<Long>("id"), specialtyId) else null
    }

    private fun byDoctorId(doctorId: Long?) = Specification<Report> { root, _, cb ->
        if (doctorId != null) cb.equal(root.get<Any>("doctor").get<Long>("id"), doctorId) else null
    }

    private fun buildSpec(
        title: String?,
        patientId: Long?,
        specialtyId: Long?,
        doctorId: Long?
    ): Specification<Report> =
        Specification.where(byTitle(title))
            .and(byPatientId(patientId))
            .and(bySpecialtyId(specialtyId))
            .and(byDoctorId(doctorId))

    private fun buildPageable(page: Int, size: Int, sortBy: String?, sortDirection: String?): Pageable {
        val field = if (sortBy != null && allowedSortFields.contains(sortBy)) sortBy else "date"
        val direction = try {
            Sort.Direction.fromString(sortDirection ?: "desc")
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid sortDirection '{}', defaulting to DESC", sortDirection)
            Sort.Direction.DESC
        }
        return PageRequest.of(page, size, Sort.by(direction, field))
    }

    // ========================= CRUD METHODS =========================

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
            if (doctor.specialty != specialty) {
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
                val creatorName = "${doctor.firstName} ${doctor.lastName}"
                val notification = Notification(
                    message = "Se ha creado un nuevo reporte por $creatorName para tu paciente ${savedReport.patient.firstName} ${savedReport.patient.lastName}: ${savedReport.title}",
                    user = savedReport.doctor,
                    reportId = savedReport.id
                )
                notificationService.create(notification)
            } catch (e: Exception) {
                logger.error("Error creating notification for report ${savedReport.id}", e)
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
            if (!isAdmin(doctor) && doctor.specialty != newSpecialty) {
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
                        logger.info("Attempting to delete old report file: {}", report.fileUrl)
                        val deleted = cloudinaryService.deleteFileEnhanced(report.fileUrl, "raw")
                        if (!deleted) {
                            logger.warn("Failed to delete old report file from Cloudinary: {}", report.fileUrl)
                        } else {
                            logger.info("Successfully deleted old report file from Cloudinary")
                        }
                    } catch (e: Exception) {
                        logger.error("Error eliminando archivo anterior {}", report.fileUrl, e)
                    }
                }

                newFileUrl = uploadedFileUrl
            } catch (e: Exception) {
                logger.error("Error actualizando archivo del reporte", e)
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
                val editorName = "${doctor.firstName} ${doctor.lastName}"
                val notification = Notification(
                    message = "Se ha modificado por $editorName tu reporte del paciente ${savedReport.patient.firstName} ${savedReport.patient.lastName}: ${savedReport.title}",
                    user = savedReport.doctor,
                    reportId = savedReport.id
                )
                notificationService.create(notification)
                asyncNotificationService.sendReportModifiedEmailAsync(savedReport, doctor)
            } catch (e: Exception) {
                logger.error("Error creating notification for report update ${savedReport.id}", e)
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
                logger.info("Attempting to delete report file: {}", report.fileUrl)
                val deleted = cloudinaryService.deleteFileEnhanced(report.fileUrl, "raw")
                if (!deleted) {
                    logger.warn("Failed to delete report file from Cloudinary: {}", report.fileUrl)
                } else {
                    logger.info("Successfully deleted report file from Cloudinary")
                }
            } catch (e: Exception) {
                logger.error("Error eliminando archivo {}", report.fileUrl, e)
            }

            // Eliminar reporte de la base de datos
            reportRepository.deleteById(id)
            true
        } catch (e: Exception) {
            logger.error("Error eliminando reporte {}", id, e)
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

    // ========================= PAGINATED METHODS =========================

    /**
     * Get all reports with pagination, optional filters, and sorting (for admins).
     * Uses composable JPA Specifications instead of a combinatorial when chain.
     */
    fun getAllReportsPaginated(
        patientId: Long? = null,
        specialtyId: Long? = null,
        doctorId: Long? = null,
        title: String? = null,
        page: Int = 0,
        size: Int = 10,
        sortBy: String? = null,
        sortDirection: String? = null
    ): Page<Report> {
        val titleFilter = if (title.isNullOrBlank()) null else title.trim()
        val spec = buildSpec(titleFilter, patientId, specialtyId, doctorId)
        val pageable = buildPageable(page, size, sortBy, sortDirection)
        return reportRepository.findAll(spec, pageable)
    }

    // Overload for callers that already built a Pageable (backward compat — sort fields ignored)
    fun getAllReportsPaginated(
        patientId: Long? = null,
        specialtyId: Long? = null,
        doctorId: Long? = null,
        title: String? = null,
        pageable: Pageable
    ): Page<Report> {
        val titleFilter = if (title.isNullOrBlank()) null else title.trim()
        val spec = buildSpec(titleFilter, patientId, specialtyId, doctorId)
        return reportRepository.findAll(spec, pageable)
    }

    /**
     * Get reports for a doctor with pagination, optional filters, and sorting.
     * Uses composable JPA Specifications instead of a combinatorial when chain.
     */
    fun getReportsForDoctorPaginated(
        doctor: AppUser,
        patientId: Long? = null,
        specialtyId: Long? = null,
        doctorId: Long? = null,
        title: String? = null,
        page: Int = 0,
        size: Int = 10,
        sortBy: String? = null,
        sortDirection: String? = null
    ): Page<Report> {
        val titleFilter = if (title.isNullOrBlank()) null else title.trim()
        // If doctorId is not specified, default to the current doctor's reports
        val effectiveDoctorId = doctorId ?: doctor.id
        val spec = buildSpec(titleFilter, patientId, specialtyId, effectiveDoctorId)
        val pageable = buildPageable(page, size, sortBy, sortDirection)
        return reportRepository.findAll(spec, pageable)
    }

    // Overload for callers that already built a Pageable (backward compat — sort fields ignored)
    fun getReportsForDoctorPaginated(
        doctor: AppUser,
        patientId: Long? = null,
        specialtyId: Long? = null,
        doctorId: Long? = null,
        title: String? = null,
        pageable: Pageable
    ): Page<Report> {
        val titleFilter = if (title.isNullOrBlank()) null else title.trim()
        val effectiveDoctorId = doctorId ?: doctor.id
        val spec = buildSpec(titleFilter, patientId, specialtyId, effectiveDoctorId)
        return reportRepository.findAll(spec, pageable)
    }
}
