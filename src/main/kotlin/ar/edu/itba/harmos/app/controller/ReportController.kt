package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.security.annotations.CurrentUser
import ar.edu.itba.harmos.services.CloudinaryService
import ar.edu.itba.harmos.services.PatientService
import ar.edu.itba.harmos.services.ReportService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/reports")
class ReportController(
    private val reportService: ReportService,
    private val patientService: PatientService,
    private val cloudinaryService: CloudinaryService
) {

    @PostMapping("/{id}/files")
    fun uploadReportFiles(
        @PathVariable id: Long,
        @RequestParam("images", required = false) images: Array<MultipartFile>?,
        @RequestParam("files", required = false) files: Array<MultipartFile>?,
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

        return try {
            val uploadedImages = mutableListOf<Map<String, Any>>()
            val uploadedFiles = mutableListOf<Map<String, Any>>()
            val errors = mutableListOf<String>()

            // Subir imágenes médicas (rayos X, resonancias, etc.)
            images?.forEach { image ->
                try {
                    val imageUrl = cloudinaryService.uploadImage(image, "reports")
                    val publicId = cloudinaryService.extractPublicId(imageUrl)
                    report.images.add(imageUrl)
                    
                    uploadedImages.add(mapOf(
                        "url" to imageUrl,
                        "public_id" to publicId,
                        "variants" to cloudinaryService.getImageVariants(publicId),
                        "filename" to (image.originalFilename ?: "unknown")
                    ))
                } catch (e: Exception) {
                    errors.add("Error con imagen ${image.originalFilename}: ${e.message}")
                }
            }

            // Subir documentos médicos (PDFs, etc.)
            files?.forEach { file ->
                try {
                    val fileUrl = cloudinaryService.uploadDocument(file, "reports")
                    val publicId = cloudinaryService.extractPublicId(fileUrl)
                    report.files.add(fileUrl)
                    
                    val originalFilename = file.originalFilename ?: "unknown"
                    uploadedFiles.add(mapOf(
                        "url" to fileUrl,
                        "download_url" to cloudinaryService.getDownloadUrl(publicId, originalFilename),
                        "public_id" to publicId,
                        "filename" to originalFilename,
                        "size" to file.size
                    ))
                } catch (e: Exception) {
                    errors.add("Error con archivo ${file.originalFilename}: ${e.message}")
                }
            }

            // Guardar cambios
            reportService.updateReportFiles(report)

            val response = mutableMapOf<String, Any>(
                "images" to uploadedImages,
                "files" to uploadedFiles,
                "uploaded" to (uploadedImages.size + uploadedFiles.size)
            )

            if (errors.isNotEmpty()) {
                response["errors"] = errors
            }

            ResponseEntity(response, HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(
                mapOf("error" to "Error al subir archivos: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @DeleteMapping("/{id}/files")
    fun deleteReportFile(
        @PathVariable id: Long,
        @RequestParam("fileUrl") fileUrl: String,
        @RequestParam("type") type: String, // "image" o "file"
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

        return try {
            val publicId = cloudinaryService.extractPublicId(fileUrl)
            val resourceType = if (type == "image") "image" else "raw"
            
            // Eliminar de Cloudinary
            val deleted = cloudinaryService.deleteFile(publicId, resourceType)
            if (!deleted) {
                return ResponseEntity(mapOf("error" to "No se pudo eliminar el archivo"), HttpStatus.NOT_FOUND)
            }

            // Eliminar de la base de datos
            when (type) {
                "image" -> report.images.remove(fileUrl)
                "file" -> report.files.remove(fileUrl)
                else -> return ResponseEntity(mapOf("error" to "Tipo de archivo inválido"), HttpStatus.BAD_REQUEST)
            }

            reportService.updateReportFiles(report)
            ResponseEntity(mapOf("message" to "Archivo eliminado correctamente"), HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(
                mapOf("error" to "Error al eliminar archivo: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @GetMapping("/{id}/files")
    fun getReportFiles(
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
            val imagesWithVariants = report.images.map { imageUrl ->
                val publicId = cloudinaryService.extractPublicId(imageUrl)
                mapOf(
                    "url" to imageUrl,
                    "public_id" to publicId,
                    "variants" to cloudinaryService.getImageVariants(publicId)
                )
            }

            val filesWithInfo = report.files.map { fileUrl ->
                val publicId = cloudinaryService.extractPublicId(fileUrl)
                val originalFilename = cloudinaryService.extractFilenameFromPublicId(publicId)
                mapOf(
                    "url" to fileUrl,
                    "download_url" to cloudinaryService.getDownloadUrl(publicId, originalFilename),
                    "public_id" to publicId,
                    "filename" to originalFilename
                )
            }

            val response = mapOf(
                "images" to imagesWithVariants,
                "files" to filesWithInfo,
                "total" to (report.images.size + report.files.size)
            )

            return ResponseEntity(response, HttpStatus.OK)
        } catch (e: Exception) {
            return ResponseEntity(
                mapOf("error" to "Error al obtener archivos: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
} 