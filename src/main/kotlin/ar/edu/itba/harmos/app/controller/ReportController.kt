package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreateReportRequest
import ar.edu.itba.harmos.dtos.requests.EditReportRequest
import ar.edu.itba.harmos.dtos.responses.ReportResponse
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.AppUserRole
import ar.edu.itba.harmos.security.annotations.CurrentUser
import ar.edu.itba.harmos.services.CloudinaryService
import ar.edu.itba.harmos.services.ReportService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
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

    // ========================= HELPER FUNCTIONS =========================

    /**
     * Check if the user has administrator role
     */
    private fun isAdmin(user: AppUser): Boolean {
        return user.roles.any { it.role == AppUserRole.ADMINISTRATOR.roleName }
    }

    // ========================= CRUD OPERATIONS =========================

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

        // Input validation
        if (title.isBlank()) {
            return ResponseEntity(mapOf("error" to "El título es obligatorio y no puede estar vacío"), HttpStatus.BAD_REQUEST)
        }

        if (title.length > 255) {
            return ResponseEntity(mapOf("error" to "El título no puede exceder 255 caracteres"), HttpStatus.BAD_REQUEST)
        }

        if (patientId <= 0) {
            return ResponseEntity(mapOf("error" to "ID del paciente inválido"), HttpStatus.BAD_REQUEST)
        }

        if (specialtyId <= 0) {
            return ResponseEntity(mapOf("error" to "ID de la especialidad inválido"), HttpStatus.BAD_REQUEST)
        }

        // File validation
        if (file.isEmpty) {
            return ResponseEntity(mapOf("error" to "El archivo es obligatorio para crear un reporte"), HttpStatus.BAD_REQUEST)
        }

        val originalFilename = file.originalFilename
        if (originalFilename.isNullOrBlank()) {
            return ResponseEntity(mapOf("error" to "El archivo debe tener un nombre válido"), HttpStatus.BAD_REQUEST)
        }

        // File size validation (25MB max)
        if (file.size > 25 * 1024 * 1024) {
            return ResponseEntity(mapOf("error" to "El archivo es muy grande (máximo 25MB)"), HttpStatus.BAD_REQUEST)
        }

        // File size minimum validation (1KB min)
        if (file.size < 1024) {
            return ResponseEntity(mapOf("error" to "El archivo es muy pequeño (mínimo 1KB)"), HttpStatus.BAD_REQUEST)
        }

        // File type validation
        val allowedContentTypes = listOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "image/jpeg",
            "image/jpg",
            "image/png"
        )

        val fileContentType = file.contentType
        if (fileContentType == null || !allowedContentTypes.contains(fileContentType)) {
            return ResponseEntity(
                mapOf("error" to "Tipo de archivo no permitido. Formatos permitidos: PDF, DOC, DOCX, TXT, JPG, JPEG, PNG"),
                HttpStatus.BAD_REQUEST
            )
        }

        // File extension validation
        val filename = originalFilename.lowercase()
        val allowedExtensions = listOf(".pdf", ".doc", ".docx", ".txt", ".jpg", ".jpeg", ".png")
        if (!allowedExtensions.any { filename.endsWith(it) }) {
            return ResponseEntity(
                mapOf("error" to "Extensión de archivo no permitida. Extensiones permitidas: ${allowedExtensions.joinToString(", ")}"),
                HttpStatus.BAD_REQUEST
            )
        }

        return try {
            // Subir archivo primero
            val fileUrl = cloudinaryService.uploadDocument(file, "reports")

            // Validar que se obtuvo una URL válida
            if (fileUrl.isBlank()) {
                return ResponseEntity(mapOf("error" to "Error al subir el archivo. Inténtelo de nuevo"), HttpStatus.INTERNAL_SERVER_ERROR)
            }

            // Crear el reporte con el archivo
            val createReportRequest = CreateReportRequest(title.trim(), patientId, specialtyId)
            val report = reportService.createReportWithFile(createReportRequest, appUser, fileUrl)
                ?: return ResponseEntity(mapOf("error" to "No se pudo crear el reporte. Verifique que el paciente y la especialidad existan"), HttpStatus.BAD_REQUEST)
            
            ResponseEntity(ReportResponse.singleFromModel(report, cloudinaryService), HttpStatus.CREATED)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to "Datos inválidos: ${e.message}"), HttpStatus.BAD_REQUEST)
        } catch (e: IllegalAccessException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.FORBIDDEN)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity(mapOf("error" to "Error interno del servidor. Inténtelo de nuevo más tarde"), HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping
    @ResponseBody
    fun getReports(
        @RequestParam(required = false) patientId: Long?,
        @RequestParam(required = false) specialtyId: Long?,
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

        if (page < 0) {
            return ResponseEntity(mapOf("error" to "El número de página debe ser mayor o igual a 0"), HttpStatus.BAD_REQUEST)
        }

        if (size <= 0 || size > 100) {
            return ResponseEntity(mapOf("error" to "El tamaño de página debe estar entre 1 y 100"), HttpStatus.BAD_REQUEST)
        }

        return try {
            val pageable = PageRequest.of(page, size)
            val reportsPage = reportService.getReportsForDoctorPaginated(appUser, patientId, specialtyId, pageable)
            
            if (reportsPage.isEmpty) {
                val message = when {
                    patientId != null && specialtyId != null -> "No se encontraron reportes para el paciente y especialidad especificados"
                    patientId != null -> "No se encontraron reportes para el paciente especificado"
                    specialtyId != null -> "No se encontraron reportes para la especialidad especificada"
                    else -> "No se encontraron reportes"
                }
                return ResponseEntity(mapOf(
                    "message" to message, 
                    "reports" to emptyList<Any>(),
                    "totalElements" to 0,
                    "totalPages" to 0,
                    "currentPage" to page,
                    "pageSize" to size
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
                "hasPrevious" to reportsPage.hasPrevious()
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to "Parámetros inválidos: ${e.message}"), HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            e.printStackTrace()
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
            if (!isAdmin(appUser) && report.doctor.id != appUser.id && !report.patient.doctors.contains(appUser)) {
                return ResponseEntity(mapOf("error" to "No tienes acceso a este reporte"), HttpStatus.FORBIDDEN)
            }

            ResponseEntity(ReportResponse.singleFromModel(report, cloudinaryService), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to "ID inválido: ${e.message}"), HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity(
                mapOf("error" to "Error al obtener el reporte. Inténtelo de nuevo más tarde"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    fun updateReport(
        @PathVariable id: Long,
        @RequestBody editReportRequest: EditReportRequest,
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

        // Input validation
        if (editReportRequest.title != null) {
            val title = editReportRequest.title
            if (title.isBlank()) {
                return ResponseEntity(mapOf("error" to "El título no puede estar vacío"), HttpStatus.BAD_REQUEST)
            }
            if (title.length > 255) {
                return ResponseEntity(mapOf("error" to "El título no puede exceder 255 caracteres"), HttpStatus.BAD_REQUEST)
            }
        }

        return try {
            // Verificar que el reporte existe antes de intentar actualizarlo
            val existingReport = reportService.getReportById(id)
            if (existingReport == null) {
                return ResponseEntity(mapOf("error" to "Reporte no encontrado"), HttpStatus.NOT_FOUND)
            }

            // Verificar que el usuario tiene permisos para actualizar el reporte
            if (!isAdmin(appUser) && existingReport.doctor.id != appUser.id) {
                return ResponseEntity(mapOf("error" to "Solo el doctor que creó el reporte o un administrador puede editarlo"), HttpStatus.FORBIDDEN)
            }

            val updatedReport = reportService.updateReport(id, editReportRequest, appUser)
                ?: return ResponseEntity(mapOf("error" to "No se pudo actualizar el reporte"), HttpStatus.INTERNAL_SERVER_ERROR)
            
            ResponseEntity(ReportResponse.singleFromModel(updatedReport, cloudinaryService), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to "Datos inválidos: ${e.message}"), HttpStatus.BAD_REQUEST)
        } catch (e: IllegalAccessException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.FORBIDDEN)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity(
                mapOf("error" to "Error al actualizar el reporte. Inténtelo de nuevo más tarde"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
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
            if (!isAdmin(appUser) && existingReport.doctor.id != appUser.id) {
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
            e.printStackTrace()
            ResponseEntity(
                mapOf("error" to "Error al eliminar el reporte. Inténtelo de nuevo más tarde"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    // ========================= FILE OPERATIONS =========================

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
            if (!isAdmin(appUser) && report.doctor.id != appUser.id && !report.patient.doctors.contains(appUser)) {
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
            e.printStackTrace()
            ResponseEntity(
                mapOf("error" to "Error al obtener el archivo del reporte. Inténtelo de nuevo más tarde"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @GetMapping("/all")
    @ResponseBody
    fun getAllReportsForAdmin(
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

        // Admin authorization check
        if (!isAdmin(appUser)) {
            return ResponseEntity(mapOf("error" to "Solo los administradores pueden acceder a todos los reportes"), HttpStatus.FORBIDDEN)
        }

        // Parameter validation
        if (page < 0) {
            return ResponseEntity(mapOf("error" to "El número de página debe ser mayor o igual a 0"), HttpStatus.BAD_REQUEST)
        }

        if (size <= 0 || size > 100) {
            return ResponseEntity(mapOf("error" to "El tamaño de página debe estar entre 1 y 100"), HttpStatus.BAD_REQUEST)
        }

        return try {
            val pageable = PageRequest.of(page, size)
            val reportsPage = reportService.getAllReportsPaginated(pageable)
            
            if (reportsPage.isEmpty) {
                return ResponseEntity(mapOf(
                    "message" to "No se encontraron reportes en el sistema", 
                    "reports" to emptyList<Any>(),
                    "totalElements" to 0,
                    "totalPages" to 0,
                    "currentPage" to page,
                    "pageSize" to size
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
                "hasPrevious" to reportsPage.hasPrevious()
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to "Parámetros inválidos: ${e.message}"), HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity(
                mapOf("error" to "Error al obtener reportes. Inténtelo de nuevo más tarde"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
} 