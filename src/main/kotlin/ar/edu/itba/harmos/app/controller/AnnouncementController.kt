package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreateAnnouncementRequest
import ar.edu.itba.harmos.dtos.requests.EditAnnouncementRequest
import ar.edu.itba.harmos.dtos.responses.AnnouncementResponse
import ar.edu.itba.harmos.dtos.responses.AppUserResponse
import ar.edu.itba.harmos.models.Announcement
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.security.annotations.CurrentUser
import ar.edu.itba.harmos.services.AnnouncementService
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.CloudinaryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


@Validated
@RestController
@RequestMapping("/announcements")
class AnnouncementController(
    private val announcementService: AnnouncementService,
    private val cloudinaryService: CloudinaryService
) {
    @PostMapping()
    @ResponseBody
    fun create(
        @RequestBody createAnnouncementRequest: CreateAnnouncementRequest,
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
            ex.printStackTrace() // O usa un logger
            ResponseEntity(mapOf("error" to (ex.message ?: "Error interno")), HttpStatus.INTERNAL_SERVER_ERROR)
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

    @PutMapping("/{id}")
    fun updateAnnouncement(
        @PathVariable id: Long,
        @RequestBody editRequest: EditAnnouncementRequest
    ): ResponseEntity<Any> {
        val updatedAnnouncement = announcementService.updateAnnouncement(id, editRequest)
        return ResponseEntity.ok(AnnouncementResponse.singleFromModel(updatedAnnouncement))
    }

    @DeleteMapping("/{id}")
    fun deleteAnnouncement(@PathVariable id: Long): ResponseEntity<Void> {
        val deleted = announcementService.deleteAnnouncement(id)
        return if (deleted) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
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
                    e.printStackTrace() // Para debugging
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
                    
                    uploadedFiles.add(mapOf(
                        "url" to fileUrl,
                        "public_id" to publicId,
                        "filename" to (file.originalFilename ?: "unknown")
                    ))
                } catch (e: IllegalArgumentException) {
                    errors.add("Archivo ${file.originalFilename}: ${e.message}")
                } catch (e: RuntimeException) {
                    errors.add("Archivo ${file.originalFilename}: Error de Cloudinary - ${e.message}")
                } catch (e: Exception) {
                    errors.add("Archivo ${file.originalFilename}: Error inesperado - ${e.javaClass.simpleName}: ${e.message}")
                    e.printStackTrace() // Para debugging
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
            val publicId = cloudinaryService.extractPublicId(fileUrl)
            val resourceType = if (type == "image") "image" else "raw"
            
            // Eliminar de Cloudinary
            val deleted = cloudinaryService.deleteFile(publicId, resourceType)
            if (!deleted) {
                return ResponseEntity(mapOf("error" to "No se pudo eliminar el archivo"), HttpStatus.NOT_FOUND)
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