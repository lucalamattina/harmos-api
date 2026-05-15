package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreateReportRequest
import ar.edu.itba.harmos.dtos.requests.EditReportRequest
import ar.edu.itba.harmos.dtos.responses.ReportResponse
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.security.annotations.CurrentUser
import ar.edu.itba.harmos.services.CloudinaryService
import ar.edu.itba.harmos.services.ReportService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Validated
@RestController
@RequestMapping("/reports")
class ReportController(
    private val reportService: ReportService,
    private val cloudinaryService: CloudinaryService
) {
    private val logger = LoggerFactory.getLogger(ReportController::class.java)

    // ====================== Upload allowlist (A.1) ======================
    // Documents: extension+MIME AND-check (mirrors CloudinaryService.isValidDocument).
    private val allowedDocumentMimeTypes = setOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    )
    private val allowedDocumentExtensions = setOf(
        ".pdf", ".doc", ".docx", ".txt", ".xls", ".xlsx", ".ppt", ".pptx"
    )
    // Images: curated subset for reports (jpg/jpeg/png only).
    private val allowedImageMimeTypes = setOf("image/jpeg", "image/jpg", "image/png")
    private val allowedImageExtensions = setOf(".jpg", ".jpeg", ".png")

    private val maxUploadBytes = 25L * 1024L * 1024L // 25MB authoritative server-side limit
    private val minUploadBytes = 1024L               // 1KB

    private data class FileValidationOk(val isImage: Boolean)

    /**
     * Validates a multipart file against the reports allowlist using an AND-check
     * (extension AND MIME must both match the same family). Returns either an OK
     * marker (with isImage flag for routing to the correct uploader) or a single
     * error message string suitable for the field-keyed `errors.file` slot.
     */
    private fun validateReportFile(file: MultipartFile): Result<FileValidationOk> {
        val originalFilename = file.originalFilename
        if (originalFilename.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("El archivo debe tener un nombre válido"))
        }
        if (file.size > maxUploadBytes) {
            return Result.failure(IllegalArgumentException("El archivo es muy grande (máximo 25MB)"))
        }
        if (file.size < minUploadBytes) {
            return Result.failure(IllegalArgumentException("El archivo es muy pequeño (mínimo 1KB)"))
        }

        val filename = originalFilename.lowercase()
        val mime = file.contentType
        val docExtMatch = allowedDocumentExtensions.any { filename.endsWith(it) }
        val docMimeMatch = mime != null && allowedDocumentMimeTypes.contains(mime)
        val imgExtMatch = allowedImageExtensions.any { filename.endsWith(it) }
        val imgMimeMatch = mime != null && allowedImageMimeTypes.contains(mime)

        return when {
            docExtMatch && docMimeMatch -> Result.success(FileValidationOk(isImage = false))
            imgExtMatch && imgMimeMatch -> Result.success(FileValidationOk(isImage = true))
            else -> Result.failure(
                IllegalArgumentException(
                    "Tipo de archivo no permitido. Formatos permitidos: " +
                        "PDF, DOC, DOCX, TXT, XLS, XLSX, PPT, PPTX, JPG, JPEG, PNG"
                )
            )
        }
    }

    /**
     * Routes the file to the correct CloudinaryService uploader based on the validation result.
     * Both branches go to the `reports` folder.
     */
    private fun uploadReportFile(file: MultipartFile, isImage: Boolean): String {
        return if (isImage) {
            cloudinaryService.uploadImage(file, "reports")
        } else {
            cloudinaryService.uploadDocument(file, "reports")
        }
    }

    @PostMapping(consumes = ["multipart/form-data"])
    @ResponseBody
    fun createReport(
        @RequestParam("title") title: String,
        @RequestParam("patientId") patientId: Long,
        @RequestParam("specialtyId") specialtyId: Long,
        @RequestParam("file") file: MultipartFile,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        // Authentication check
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        // ===== Field-keyed validation (E.1) =====
        val fieldErrors = linkedMapOf<String, String>()

        if (title.isBlank()) {
            fieldErrors["title"] = "El título es obligatorio y no puede estar vacío"
        } else if (title.length > 255) {
            fieldErrors["title"] = "El título no puede exceder 255 caracteres"
        }

        if (patientId <= 0) {
            fieldErrors["patientId"] = "ID del paciente inválido"
        }

        if (specialtyId <= 0) {
            fieldErrors["specialtyId"] = "ID de la especialidad inválido"
        }

        if (file.isEmpty) {
            fieldErrors["file"] = "El archivo es obligatorio para crear un reporte"
        } else {
            val fileValidation = validateReportFile(file)
            fileValidation.exceptionOrNull()?.let {
                fieldErrors["file"] = it.message ?: "Archivo no válido"
            }
        }

        if (fieldErrors.isNotEmpty()) {
            return ResponseEntity(mapOf("errors" to fieldErrors), HttpStatus.BAD_REQUEST)
        }

        return try {
            // Subir archivo primero (routing image vs document)
            val isImage = validateReportFile(file).getOrThrow().isImage
            val fileUrl = uploadReportFile(file, isImage)

            // Validar que se obtuvo una URL válida
            if (fileUrl.isBlank()) {
                return ResponseEntity(mapOf("error" to "Error al subir el archivo. Inténtelo de nuevo"), HttpStatus.INTERNAL_SERVER_ERROR)
            }

            // Crear el reporte con el archivo
            val createReportRequest = CreateReportRequest(title.trim(), patientId, specialtyId)
            val report = reportService.createReportWithFile(createReportRequest, appUser, fileUrl)
                ?: return ResponseEntity(
                    mapOf("errors" to mapOf("patientId" to "Paciente o especialidad no encontrados")),
                    HttpStatus.BAD_REQUEST
                )

            ResponseEntity(ReportResponse.singleFromModel(report, cloudinaryService), HttpStatus.CREATED)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to "Datos inválidos: ${e.message}"), HttpStatus.BAD_REQUEST)
        } catch (e: IllegalAccessException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.FORBIDDEN)
        } catch (e: Exception) {
            logger.error("Error creating report", e)
            ResponseEntity(mapOf("error" to "Error interno del servidor. Inténtelo de nuevo más tarde"), HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping
    @ResponseBody
    fun getReports(
        @RequestParam(required = false) patientId: Long?,
        @RequestParam(required = false) specialtyId: Long?,
        @RequestParam(required = false) doctorId: Long?,
        @RequestParam(required = false) title: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "date") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDirection: String,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        // Authentication check
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        // Parameter validation
        if (patientId != null && patientId <= 0) {
            return ResponseEntity(mapOf("error" to "ID del paciente inválido"), HttpStatus.BAD_REQUEST)
        }

        if (specialtyId != null && specialtyId <= 0) {
            return ResponseEntity(mapOf("error" to "ID de la especialidad inválido"), HttpStatus.BAD_REQUEST)
        }

        if (doctorId != null && doctorId <= 0) {
            return ResponseEntity(mapOf("error" to "ID del doctor inválido"), HttpStatus.BAD_REQUEST)
        }

        if (!title.isNullOrBlank() && title.length > 255) {
            return ResponseEntity(mapOf("error" to "El filtro de título no puede exceder 255 caracteres"), HttpStatus.BAD_REQUEST)
        }

        if (page < 0) {
            return ResponseEntity(mapOf("error" to "El número de página debe ser mayor o igual a 0"), HttpStatus.BAD_REQUEST)
        }

        if (size <= 0 || size > 100) {
            return ResponseEntity(mapOf("error" to "El tamaño de página debe estar entre 1 y 100"), HttpStatus.BAD_REQUEST)
        }

        return try {
            val reportsPage = reportService.getReportsForDoctorPaginated(
                appUser, patientId, specialtyId, doctorId, title, page, size, sortBy, sortDirection
            )

            if (reportsPage.isEmpty) {
                val filters = mutableListOf<String>()
                patientId?.let { filters.add("paciente") }
                specialtyId?.let { filters.add("especialidad") }
                doctorId?.let { filters.add("doctor") }
                if (!title.isNullOrBlank()) filters.add("título")

                val message = if (filters.isNotEmpty()) {
                    "No se encontraron reportes para los filtros especificados: ${filters.joinToString(", ")}"
                } else {
                    "No se encontraron reportes"
                }

                return ResponseEntity(mapOf(
                    "message" to message,
                    "reports" to emptyList<Any>(),
                    "totalElements" to 0,
                    "totalPages" to 0,
                    "currentPage" to page,
                    "pageSize" to size,
                    "appliedFilters" to mapOf(
                        "patientId" to patientId,
                        "specialtyId" to specialtyId,
                        "doctorId" to doctorId,
                        "title" to title
                    )
                ), HttpStatus.OK)
            }

            val response = ReportResponse.listFromModel(reportsPage.content, cloudinaryService)
            ResponseEntity.ok(mapOf(
                "reports" to response,
                "totalElements" to reportsPage.totalElements,
                "totalPages" to reportsPage.totalPages,
                "currentPage" to reportsPage.number,
                "pageSize" to reportsPage.size,
                "hasNext" to reportsPage.hasNext(),
                "hasPrevious" to reportsPage.hasPrevious(),
                "appliedFilters" to mapOf(
                    "patientId" to patientId,
                    "specialtyId" to specialtyId,
                    "doctorId" to doctorId,
                    "title" to title
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to "Parámetros inválidos: ${e.message}"), HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            logger.error("Error fetching reports for doctor", e)
            ResponseEntity(
                mapOf("error" to "Error al obtener reportes. Inténtelo de nuevo más tarde"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @GetMapping("/{id}")
    @ResponseBody
    fun getReportById(
        @PathVariable id: Long,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        // Authentication check
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        // ID validation
        if (id <= 0) {
            return ResponseEntity(mapOf("error" to "ID del reporte inválido"), HttpStatus.BAD_REQUEST)
        }

        return try {
            val report = reportService.getReportById(id)
                ?: return ResponseEntity(mapOf("error" to "Reporte no encontrado"), HttpStatus.NOT_FOUND)

            // Verificar que el usuario tiene acceso al reporte
            if (!reportService.isAdmin(appUser) && report.doctor.id != appUser.id && !report.patient.doctors.contains(appUser)) {
                return ResponseEntity(mapOf("error" to "No tienes acceso a este reporte"), HttpStatus.FORBIDDEN)
            }

            ResponseEntity(ReportResponse.singleFromModel(report, cloudinaryService), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to "ID inválido: ${e.message}"), HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            logger.error("Error fetching report by id", e)
            ResponseEntity(
                mapOf("error" to "Error al obtener el reporte. Inténtelo de nuevo más tarde"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @PutMapping("/{id}", consumes = ["multipart/form-data"])
    @ResponseBody
    fun updateReport(
        @PathVariable id: Long,
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) patientId: Long?,
        @RequestParam(required = false) specialtyId: Long?,
        @RequestParam(required = false) file: MultipartFile?,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        // Authentication check
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        // ID validation
        if (id <= 0) {
            return ResponseEntity(mapOf("error" to "ID del reporte inválido"), HttpStatus.BAD_REQUEST)
        }

        // Verificar que el reporte existe
        val existingReport = reportService.getReportById(id)
            ?: return ResponseEntity(mapOf("error" to "Reporte no encontrado"), HttpStatus.NOT_FOUND)

        // Verificar que el usuario tiene permisos para editar el reporte
        if (!reportService.isAdmin(appUser) && existingReport.doctor.id != appUser.id) {
            return ResponseEntity(mapOf("error" to "Solo el doctor que creó el reporte o un administrador puede editarlo"), HttpStatus.FORBIDDEN)
        }

        // ===== Field-keyed validation (E.1) =====
        val fieldErrors = linkedMapOf<String, String>()

        if (title != null) {
            if (title.isBlank()) {
                fieldErrors["title"] = "El título no puede estar vacío"
            } else if (title.length > 255) {
                fieldErrors["title"] = "El título no puede exceder 255 caracteres"
            }
        }

        if (patientId != null && patientId <= 0) {
            fieldErrors["patientId"] = "ID del paciente inválido"
        }

        if (specialtyId != null && specialtyId <= 0) {
            fieldErrors["specialtyId"] = "ID de la especialidad inválido"
        }

        // File validation (si se proporciona un archivo)
        if (file != null && !file.isEmpty) {
            val fileValidation = validateReportFile(file)
            fileValidation.exceptionOrNull()?.let {
                fieldErrors["file"] = it.message ?: "Archivo no válido"
            }
        }

        if (fieldErrors.isNotEmpty()) {
            return ResponseEntity(mapOf("errors" to fieldErrors), HttpStatus.BAD_REQUEST)
        }

        // Validar que al menos un campo se está actualizando
        if (title == null && patientId == null && specialtyId == null && (file == null || file.isEmpty)) {
            return ResponseEntity(mapOf("error" to "Debe proporcionar al menos un campo para actualizar"), HttpStatus.BAD_REQUEST)
        }

        return try {
            val editReportRequest = EditReportRequest(
                title = title?.trim(),
                patientId = patientId,
                specialtyId = specialtyId
            )

            val updatedReport = reportService.updateReport(id, editReportRequest, appUser, file)
                ?: return ResponseEntity(mapOf("error" to "No se pudo actualizar el reporte"), HttpStatus.INTERNAL_SERVER_ERROR)

            ResponseEntity(ReportResponse.singleFromModel(updatedReport, cloudinaryService), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to "Datos inválidos: ${e.message}"), HttpStatus.BAD_REQUEST)
        } catch (e: IllegalAccessException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.FORBIDDEN)
        } catch (e: RuntimeException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.INTERNAL_SERVER_ERROR)
        } catch (e: Exception) {
            logger.error("Error updating report", e)
            ResponseEntity(mapOf("error" to "Error interno del servidor. Inténtelo de nuevo más tarde"), HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @DeleteMapping("/{id}")
    fun deleteReport(
        @PathVariable id: Long,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        // Authentication check
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        // ID validation
        if (id <= 0) {
            return ResponseEntity(mapOf("error" to "ID del reporte inválido"), HttpStatus.BAD_REQUEST)
        }

        return try {
            // Verificar que el reporte existe antes de intentar eliminarlo
            val existingReport = reportService.getReportById(id)
            if (existingReport == null) {
                return ResponseEntity(mapOf("error" to "Reporte no encontrado"), HttpStatus.NOT_FOUND)
            }

            // Verificar que el usuario tiene permisos para eliminar el reporte
            if (!reportService.isAdmin(appUser) && existingReport.doctor.id != appUser.id) {
                return ResponseEntity(mapOf("error" to "Solo el doctor que creó el reporte o un administrador puede eliminarlo"), HttpStatus.FORBIDDEN)
            }

            val deleted = reportService.deleteReport(id, appUser)
            if (deleted) {
                ResponseEntity(mapOf("message" to "Reporte eliminado correctamente"), HttpStatus.OK)
            } else {
                ResponseEntity(mapOf("error" to "No se pudo eliminar el reporte"), HttpStatus.INTERNAL_SERVER_ERROR)
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to "ID inválido: ${e.message}"), HttpStatus.BAD_REQUEST)
        } catch (e: IllegalAccessException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.FORBIDDEN)
        } catch (e: Exception) {
            logger.error("Error deleting report", e)
            ResponseEntity(
                mapOf("error" to "Error al eliminar el reporte. Inténtelo de nuevo más tarde"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }



    @GetMapping("/{id}/file")
    fun getReportFile(
        @PathVariable id: Long,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        // Authentication check
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        // ID validation
        if (id <= 0) {
            return ResponseEntity(mapOf("error" to "ID del reporte inválido"), HttpStatus.BAD_REQUEST)
        }

        return try {
            val report = reportService.getReportById(id)
                ?: return ResponseEntity(mapOf("error" to "Reporte no encontrado"), HttpStatus.NOT_FOUND)

            // Verificar que el usuario tiene acceso al reporte
            if (!reportService.isAdmin(appUser) && report.doctor.id != appUser.id && !report.patient.doctors.contains(appUser)) {
                return ResponseEntity(mapOf("error" to "No tienes acceso a este reporte"), HttpStatus.FORBIDDEN)
            }

            // Verificar que el reporte tiene un archivo asociado
            if (report.fileUrl.isBlank()) {
                return ResponseEntity(mapOf("error" to "Este reporte no tiene archivo asociado"), HttpStatus.NOT_FOUND)
            }

            val publicId = cloudinaryService.extractPublicId(report.fileUrl)
            val originalFilename = cloudinaryService.extractFilenameFromUrl(report.fileUrl)
            val resourceType = if (report.fileUrl.contains("/image/upload/")) "image" else "raw"
            val documentType = cloudinaryService.getDocumentType(originalFilename)
            
            // Verificar que se pudo extraer información válida del archivo
            if (publicId.isBlank() || originalFilename.isBlank()) {
                return ResponseEntity(mapOf("error" to "Información del archivo inválida"), HttpStatus.INTERNAL_SERVER_ERROR)
            }
            
            val fileInfo = mutableMapOf<String, Any>(
                "url" to report.fileUrl,
                "download_url" to cloudinaryService.getDownloadUrl(publicId, originalFilename, report.fileUrl),
                "direct_url" to cloudinaryService.getDirectUrl(publicId, report.fileUrl),
                "public_id" to publicId,
                "filename" to originalFilename,
                "resource_type" to resourceType,
                "document_type" to documentType
            )
            
            ResponseEntity.ok(fileInfo)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to "ID inválido: ${e.message}"), HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            logger.error("Error fetching report file", e)
            ResponseEntity(
                mapOf("error" to "Error al obtener el archivo del reporte. Inténtelo de nuevo más tarde"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @GetMapping("/all")
    @ResponseBody
    fun getAllReportsForAdmin(
        @RequestParam(required = false) patientId: Long?,
        @RequestParam(required = false) specialtyId: Long?,
        @RequestParam(required = false) doctorId: Long?,
        @RequestParam(required = false) title: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "date") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDirection: String,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        // Authentication check
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        // Parameter validation
        if (patientId != null && patientId <= 0) {
            return ResponseEntity(mapOf("error" to "ID del paciente inválido"), HttpStatus.BAD_REQUEST)
        }

        if (specialtyId != null && specialtyId <= 0) {
            return ResponseEntity(mapOf("error" to "ID de la especialidad inválido"), HttpStatus.BAD_REQUEST)
        }

        if (doctorId != null && doctorId <= 0) {
            return ResponseEntity(mapOf("error" to "ID del doctor inválido"), HttpStatus.BAD_REQUEST)
        }

        if (!title.isNullOrBlank() && title.length > 255) {
            return ResponseEntity(mapOf("error" to "El filtro de título no puede exceder 255 caracteres"), HttpStatus.BAD_REQUEST)
        }

        if (page < 0) {
            return ResponseEntity(mapOf("error" to "El número de página debe ser mayor o igual a 0"), HttpStatus.BAD_REQUEST)
        }

        if (size <= 0 || size > 100) {
            return ResponseEntity(mapOf("error" to "El tamaño de página debe estar entre 1 y 100"), HttpStatus.BAD_REQUEST)
        }

        return try {
            val reportsPage = reportService.getAllReportsPaginated(
                patientId, specialtyId, doctorId, title, page, size, sortBy, sortDirection
            )

            if (reportsPage.isEmpty) {
                val filters = mutableListOf<String>()
                patientId?.let { filters.add("paciente") }
                specialtyId?.let { filters.add("especialidad") }
                doctorId?.let { filters.add("doctor") }
                if (!title.isNullOrBlank()) filters.add("título")

                val message = if (filters.isNotEmpty()) {
                    "No se encontraron reportes en el sistema para los filtros especificados: ${filters.joinToString(", ")}"
                } else {
                    "No se encontraron reportes en el sistema"
                }

                return ResponseEntity(mapOf(
                    "message" to message,
                    "reports" to emptyList<Any>(),
                    "totalElements" to 0,
                    "totalPages" to 0,
                    "currentPage" to page,
                    "pageSize" to size,
                    "appliedFilters" to mapOf(
                        "patientId" to patientId,
                        "specialtyId" to specialtyId,
                        "doctorId" to doctorId,
                        "title" to title
                    )
                ), HttpStatus.OK)
            }

            val response = ReportResponse.listFromModel(reportsPage.content, cloudinaryService)
            ResponseEntity.ok(mapOf(
                "reports" to response,
                "totalElements" to reportsPage.totalElements,
                "totalPages" to reportsPage.totalPages,
                "currentPage" to reportsPage.number,
                "pageSize" to reportsPage.size,
                "hasNext" to reportsPage.hasNext(),
                "hasPrevious" to reportsPage.hasPrevious(),
                "appliedFilters" to mapOf(
                    "patientId" to patientId,
                    "specialtyId" to specialtyId,
                    "doctorId" to doctorId,
                    "title" to title
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to "Parámetros inválidos: ${e.message}"), HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            logger.error("Error fetching all reports for admin", e)
            ResponseEntity(
                mapOf("error" to "Error al obtener reportes. Inténtelo de nuevo más tarde"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
} 