package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreateReportRequest
import ar.edu.itba.harmos.dtos.requests.EditReportRequest
import ar.edu.itba.harmos.dtos.responses.ReportResponse
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.security.annotations.CurrentUser
import ar.edu.itba.harmos.services.CloudinaryService
import ar.edu.itba.harmos.services.ReportService
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
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        // Validar que se proporcione el archivo
        if (file.isEmpty) {
            return ResponseEntity(mapOf("error" to "El archivo es obligatorio para crear un reporte"), HttpStatus.BAD_REQUEST)
        }

        if (file.originalFilename.isNullOrBlank()) {
            return ResponseEntity(mapOf("error" to "El archivo debe tener un nombre válido"), HttpStatus.BAD_REQUEST)
        }

        if (file.size > 25 * 1024 * 1024) {
            return ResponseEntity(mapOf("error" to "El archivo es muy grande (máximo 25MB)"), HttpStatus.BAD_REQUEST)
        }

        return try {
            // Subir archivo primero
            val fileUrl = cloudinaryService.uploadDocument(file, "reports")

            // Crear el reporte con el archivo
            val createReportRequest = CreateReportRequest(title, patientId, specialtyId)
            val report = reportService.createReportWithFile(createReportRequest, appUser, fileUrl)
                ?: return ResponseEntity(mapOf("error" to "No se pudo crear el reporte"), HttpStatus.BAD_REQUEST)
            
            ResponseEntity(ReportResponse.singleFromModel(report, cloudinaryService), HttpStatus.CREATED)
        } catch (e: IllegalAccessException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.FORBIDDEN)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity(mapOf("error" to "Error interno del servidor: ${e.message}"), HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping
    @ResponseBody
    fun getReports(
        @RequestParam(required = false) patientId: Long?,
        @RequestParam(required = false) specialtyId: Long?,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        return try {
            val reports = reportService.getReportsForDoctor(appUser, patientId, specialtyId)
            val response = ReportResponse.listFromModel(reports, cloudinaryService)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity(
                mapOf("error" to "Error al obtener reportes: ${e.message}"),
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
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        val report = reportService.getReportById(id)
            ?: return ResponseEntity(mapOf("error" to "Reporte no encontrado"), HttpStatus.NOT_FOUND)

        // Verificar que el usuario tiene acceso al reporte
        if (report.doctor.id != appUser.id && !report.patient.doctors.contains(appUser)) {
            return ResponseEntity(mapOf("error" to "No tienes acceso a este reporte"), HttpStatus.FORBIDDEN)
        }

        return ResponseEntity(ReportResponse.singleFromModel(report, cloudinaryService), HttpStatus.OK)
    }

    @PutMapping("/{id}")
    @ResponseBody
    fun updateReport(
        @PathVariable id: Long,
        @RequestBody editReportRequest: EditReportRequest,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        return try {
            val updatedReport = reportService.updateReport(id, editReportRequest, appUser)
                ?: return ResponseEntity(mapOf("error" to "Reporte no encontrado"), HttpStatus.NOT_FOUND)
            
            ResponseEntity(ReportResponse.singleFromModel(updatedReport, cloudinaryService), HttpStatus.OK)
        } catch (e: IllegalAccessException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.FORBIDDEN)
        } catch (e: Exception) {
            ResponseEntity(
                mapOf("error" to "Error al actualizar reporte: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @DeleteMapping("/{id}")
    fun deleteReport(
        @PathVariable id: Long,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        return try {
            val deleted = reportService.deleteReport(id, appUser)
            if (deleted) {
                ResponseEntity(mapOf("message" to "Reporte eliminado correctamente"), HttpStatus.OK)
            } else {
                ResponseEntity(mapOf("error" to "Reporte no encontrado"), HttpStatus.NOT_FOUND)
            }
        } catch (e: IllegalAccessException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.FORBIDDEN)
        } catch (e: Exception) {
            ResponseEntity(
                mapOf("error" to "Error al eliminar reporte: ${e.message}"),
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
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        val report = reportService.getReportById(id)
            ?: return ResponseEntity(mapOf("error" to "Reporte no encontrado"), HttpStatus.NOT_FOUND)

        // Verificar que el usuario tiene acceso al reporte
        if (report.doctor.id != appUser.id && !report.patient.doctors.contains(appUser)) {
            return ResponseEntity(mapOf("error" to "No tienes acceso a este reporte"), HttpStatus.FORBIDDEN)
        }

        try {
            val publicId = cloudinaryService.extractPublicId(report.fileUrl)
            val originalFilename = cloudinaryService.extractFilenameFromUrl(report.fileUrl)
            val resourceType = if (report.fileUrl.contains("/image/upload/")) "image" else "raw"
            val documentType = cloudinaryService.getDocumentType(originalFilename)
            
            val fileInfo = mutableMapOf<String, Any>(
                "url" to report.fileUrl,
                "download_url" to cloudinaryService.getDownloadUrl(publicId, originalFilename, report.fileUrl),
                "direct_url" to cloudinaryService.getDirectUrl(publicId, report.fileUrl),
                "public_id" to publicId,
                "filename" to originalFilename,
                "resource_type" to resourceType,
                "document_type" to documentType
            )
            
            // Agregar previews para documentos
            if (documentType in listOf("pdf", "word", "excel", "powerpoint")) {
                fileInfo["preview_url"] = cloudinaryService.getDocumentPreviewUrl(publicId, report.fileUrl)
                fileInfo["thumbnail_url"] = cloudinaryService.getDocumentThumbnail(publicId, report.fileUrl)
            }

            return ResponseEntity(fileInfo.toMap(), HttpStatus.OK)
        } catch (e: Exception) {
            return ResponseEntity(
                mapOf("error" to "Error al obtener información del archivo: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    // ========================= PATIENT-SPECIFIC ENDPOINTS =========================

    @GetMapping("/patient/{patientId}")
    @ResponseBody
    fun getReportsByPatient(
        @PathVariable patientId: Long,
        @RequestParam(required = false) specialtyId: Long?,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        return try {
            val reports = reportService.getReportsForDoctor(appUser, patientId, specialtyId)
            val response = ReportResponse.listFromModel(reports, cloudinaryService)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity(
                mapOf("error" to "Error al obtener reportes del paciente: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
} 