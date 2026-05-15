package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreateAnnouncementRequest
import ar.edu.itba.harmos.dtos.requests.EditAnnouncementRequest
import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse
import ar.edu.itba.harmos.dtos.responses.AppUserResponse
import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.AppUserRole
import ar.edu.itba.harmos.security.annotations.CurrentUser
import ar.edu.itba.harmos.services.AnnouncementService
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.CloudinaryService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.validation.Valid


@Validated
@RestController
@RequestMapping("/announcements")
class AnnouncementController(
    private val announcementService: AnnouncementService,
    private val cloudinaryService: CloudinaryService
) {
    private val logger = LoggerFactory.getLogger(AnnouncementController::class.java)

    @PostMapping()
    @ResponseBody
    fun create(
        @Valid @RequestBody createAnnouncementRequest: CreateAnnouncementRequest,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        return try {
            val announcement = announcementService.createAnnouncement(createAnnouncementRequest, appUser)
                ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
            ResponseEntity(AnnouncementResponse.singleFromModel(announcement), HttpStatus.CREATED)
        } catch (ex: Exception) {
            logger.error("Error creating announcement", ex)
            ResponseEntity(mapOf("error" to "Error interno"), HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }


    @GetMapping()
    @ResponseBody
    fun findAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) specialties: List<String>?
    ): ResponseEntity<Any> {
        val announcements = announcementService.searchAnnouncementsByPage(page, size, specialties)
        val response = announcements.map { AnnouncementResponse.singleFromModel(it) }
        return ResponseEntity.ok(response)
    }



    @GetMapping("/{id}")
    @ResponseBody
    fun getById(@PathVariable id: Long): ResponseEntity<Any> {
        val announcement = announcementService.getAnnouncementById(id)
        return if (announcement != null) {
            ResponseEntity(AnnouncementResponse.singleFromModel(announcement), HttpStatus.OK)
        } else ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @GetMapping("/{id}/files")
    fun getAnnouncementFiles(@PathVariable id: Long): ResponseEntity<Any> {
        val announcement = announcementService.getAnnouncementById(id)
            ?: return ResponseEntity(mapOf("error" to "Anuncio no encontrado"), HttpStatus.NOT_FOUND)

        try {
            val imagesWithVariants = announcement.images.map { imageUrl ->
                val publicId = cloudinaryService.extractPublicId(imageUrl)
                mapOf(
                    "url" to imageUrl,
                    "public_id" to publicId,
                    "variants" to cloudinaryService.getImageVariants(publicId)
                )
            }

            val filesWithInfo = announcement.files.map { fileUrl ->
                val publicId = cloudinaryService.extractPublicId(fileUrl)
                val originalFilename = cloudinaryService.extractFilenameFromUrl(fileUrl)
                val resourceType = if (fileUrl.contains("/image/upload/")) "image" else "raw"
                val documentType = cloudinaryService.getDocumentType(originalFilename)
                
                val fileInfo = mutableMapOf<String, Any>(
                    "url" to fileUrl,
                    "download_url" to cloudinaryService.getDownloadUrl(publicId, originalFilename, fileUrl),
                    "direct_url" to cloudinaryService.getDirectUrl(publicId, fileUrl),
                    "signed_url" to cloudinaryService.getSignedUrl(publicId, resourceType),
                    "signed_download_url" to cloudinaryService.getSignedDownloadUrl(publicId, originalFilename, resourceType),
                    "public_id" to publicId,
                    "filename" to originalFilename,
                    "resource_type" to resourceType,
                    "document_type" to documentType
                )
                
                // Agregar previews para documentos (no para imágenes)
                if (documentType in listOf("pdf", "word", "excel", "powerpoint")) {
                    fileInfo["preview_url"] = cloudinaryService.getDocumentPreviewUrl(publicId, fileUrl)
                    fileInfo["thumbnail_url"] = cloudinaryService.getDocumentThumbnail(publicId, fileUrl)
                    fileInfo["pages"] = cloudinaryService.getDocumentPages(publicId, fileUrl, 3) // Máximo 3 páginas
                }
                
                fileInfo.toMap()
            }

            val response = mapOf(
                "images" to imagesWithVariants,
                "files" to filesWithInfo,
                "total" to (announcement.images.size + announcement.files.size)
            )

            return ResponseEntity(response, HttpStatus.OK)
        } catch (e: Exception) {
            return ResponseEntity(
                mapOf("error" to "Error al obtener archivos: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @PutMapping("/{id}")
    fun updateAnnouncement(
        @PathVariable id: Long,
        @Valid @RequestBody editRequest: EditAnnouncementRequest,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        val announcement = announcementService.getAnnouncementById(id)
            ?: return ResponseEntity(mapOf("error" to "Announcement not found"), HttpStatus.NOT_FOUND)
        val isAdmin = appUser.roles.any { it.role == AppUserRole.ADMINISTRATOR.roleName }
        if (!isAdmin && announcement.createdBy.id != appUser.id) {
            return ResponseEntity(mapOf("error" to "Forbidden: you do not own this announcement"), HttpStatus.FORBIDDEN)
        }
        return try {
            val updatedAnnouncement = announcementService.updateAnnouncement(id, editRequest)
            ResponseEntity.ok(AnnouncementResponse.singleFromModel(updatedAnnouncement))
        } catch (ex: RuntimeException) {
            ResponseEntity(mapOf("error" to "Announcement not found"), HttpStatus.NOT_FOUND)
        }
    }

    @DeleteMapping("/{id}")
    fun deleteAnnouncement(
        @PathVariable id: Long,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        val announcement = announcementService.getAnnouncementById(id)
            ?: return ResponseEntity(mapOf("error" to "Announcement not found"), HttpStatus.NOT_FOUND)
        val isAdmin = appUser.roles.any { it.role == AppUserRole.ADMINISTRATOR.roleName }
        if (!isAdmin && announcement.createdBy.id != appUser.id) {
            return ResponseEntity(mapOf("error" to "Forbidden: you do not own this announcement"), HttpStatus.FORBIDDEN)
        }
        val deleted = announcementService.deleteAnnouncement(id)
        return if (deleted) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity(mapOf("error" to "Announcement not found"), HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/{id}/files")
    fun uploadAnnouncementFiles(
        @PathVariable id: Long,
        @RequestParam("images", required = false) images: Array<MultipartFile>?,
        @RequestParam("files", required = false) files: Array<MultipartFile>?,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        val announcement = announcementService.getAnnouncementById(id)
            ?: return ResponseEntity(mapOf("error" to "Anuncio no encontrado"), HttpStatus.NOT_FOUND)

        return try {
            val uploadedImages = mutableListOf<Map<String, Any>>()
            val uploadedFiles = mutableListOf<Map<String, Any>>()
            val errors = mutableListOf<String>()

            // Subir imágenes
            images?.forEach { image ->
                try {
                    // Validaciones previas
                    if (image.isEmpty) {
                        errors.add("Imagen ${image.originalFilename ?: "sin nombre"}: El archivo está vacío")
                        return@forEach
                    }
                    
                    if (image.originalFilename.isNullOrBlank()) {
                        errors.add("Una imagen no tiene nombre de archivo válido")
                        return@forEach
                    }
                    
                    if (image.size > 10 * 1024 * 1024) {
                        errors.add("Imagen ${image.originalFilename}: El archivo es muy grande (máximo 10MB)")
                        return@forEach
                    }
                    
                    val imageUrl = cloudinaryService.uploadImage(image, "announcements")
                    val publicId = cloudinaryService.extractPublicId(imageUrl)
                    announcement.images.add(imageUrl)
                    
                    uploadedImages.add(mapOf(
                        "url" to imageUrl,
                        "public_id" to publicId,
                        "variants" to cloudinaryService.getImageVariants(publicId)
                    ))
                } catch (e: IllegalArgumentException) {
                    errors.add("Imagen ${image.originalFilename}: ${e.message}")
                } catch (e: RuntimeException) {
                    errors.add("Imagen ${image.originalFilename}: Error de Cloudinary - ${e.message}")
                } catch (e: Exception) {
                    errors.add("Imagen ${image.originalFilename}: Error inesperado - ${e.javaClass.simpleName}: ${e.message}")
                    logger.error("Unexpected error uploading file", e)
                }
            }

            // Subir archivos
            files?.forEach { file ->
                try {
                    // Validaciones previas
                    if (file.isEmpty) {
                        errors.add("Archivo ${file.originalFilename ?: "sin nombre"}: El archivo está vacío")
                        return@forEach
                    }
                    
                    if (file.originalFilename.isNullOrBlank()) {
                        errors.add("Un archivo no tiene nombre válido")
                        return@forEach
                    }
                    
                    if (file.size > 50 * 1024 * 1024) {
                        errors.add("Archivo ${file.originalFilename}: El archivo es muy grande (máximo 50MB)")
                        return@forEach
                    }
                    
                    val fileUrl = cloudinaryService.uploadDocument(file, "announcements")
                    val publicId = cloudinaryService.extractPublicId(fileUrl)
                    announcement.files.add(fileUrl)
                    
                    val originalFilename = file.originalFilename ?: "unknown"
                    uploadedFiles.add(mapOf(
                        "url" to fileUrl,
                        "download_url" to cloudinaryService.getDownloadUrl(publicId, originalFilename, fileUrl),
                        "direct_url" to cloudinaryService.getDirectUrl(publicId, fileUrl),
                        "public_id" to publicId,
                        "filename" to originalFilename
                    ))
                } catch (e: IllegalArgumentException) {
                    errors.add("Archivo ${file.originalFilename}: ${e.message}")
                } catch (e: RuntimeException) {
                    errors.add("Archivo ${file.originalFilename}: Error de Cloudinary - ${e.message}")
                } catch (e: Exception) {
                    errors.add("Archivo ${file.originalFilename}: Error inesperado - ${e.javaClass.simpleName}: ${e.message}")
                    logger.error("Unexpected error uploading file", e)
                }
            }

            // Guardar cambios
            announcementService.updateAnnouncementFiles(announcement)

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
    fun deleteAnnouncementFile(
        @PathVariable id: Long,
        @RequestParam("fileUrl") fileUrl: String,
        @RequestParam("type") type: String, // "image" o "file"
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        val announcement = announcementService.getAnnouncementById(id)
            ?: return ResponseEntity(mapOf("error" to "Anuncio no encontrado"), HttpStatus.NOT_FOUND)

        return try {
            val resourceType = if (type == "image") "image" else "raw"
            
            // Eliminar de Cloudinary usando método mejorado
            val deleted = cloudinaryService.deleteFileEnhanced(fileUrl, resourceType)
            if (!deleted) {
                return ResponseEntity(mapOf("error" to "No se pudo eliminar el archivo de Cloudinary"), HttpStatus.NOT_FOUND)
            }

            // Eliminar de la base de datos
            when (type) {
                "image" -> announcement.images.remove(fileUrl)
                "file" -> announcement.files.remove(fileUrl)
                else -> return ResponseEntity(mapOf("error" to "Tipo de archivo inválido"), HttpStatus.BAD_REQUEST)
            }

            announcementService.updateAnnouncementFiles(announcement)
            ResponseEntity(mapOf("message" to "Archivo eliminado correctamente"), HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(
                mapOf("error" to "Error al eliminar archivo: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
}