package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.services.CloudinaryService
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.security.annotations.CurrentUser
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/files")
class FileController(
    private val cloudinaryService: CloudinaryService
) {

    @PostMapping("/upload/image")
    fun uploadImage(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("folder") folder: String,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        return try {
            val imageUrl = cloudinaryService.uploadImage(file, folder)
            val publicId = cloudinaryService.extractPublicId(imageUrl)
            val variants = cloudinaryService.getImageVariants(publicId)
            
            ResponseEntity(mapOf(
                "url" to imageUrl,
                "public_id" to publicId,
                "variants" to variants
            ), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            ResponseEntity(
                mapOf("error" to "Error al subir la imagen: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @PostMapping("/upload/document")
    fun uploadDocument(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("folder") folder: String,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        return try {
            val documentUrl = cloudinaryService.uploadDocument(file, folder)
            val publicId = cloudinaryService.extractPublicId(documentUrl)
            
            ResponseEntity(mapOf(
                "url" to documentUrl,
                "public_id" to publicId,
                "filename" to file.originalFilename,
                "size" to file.size
            ), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            ResponseEntity(
                mapOf("error" to "Error al subir el documento: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @PostMapping("/upload/multiple")
    fun uploadMultipleFiles(
        @RequestParam("files") files: Array<MultipartFile>,
        @RequestParam("folder") folder: String,
        @RequestParam("type", defaultValue = "auto") type: String,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        if (files.isEmpty()) {
            return ResponseEntity(mapOf("error" to "No se enviaron archivos"), HttpStatus.BAD_REQUEST)
        }

        return try {
            val uploadedFiles = mutableListOf<Map<String, Any>>()
            val errors = mutableListOf<String>()

            files.forEach { file ->
                try {
                    val url = when (type) {
                        "image" -> cloudinaryService.uploadImage(file, folder)
                        "document" -> cloudinaryService.uploadDocument(file, folder)
                        else -> if (cloudinaryService.isValidImage(file)) {
                            cloudinaryService.uploadImage(file, folder)
                        } else {
                            cloudinaryService.uploadDocument(file, folder)
                        }
                    }
                    
                    val publicId = cloudinaryService.extractPublicId(url)
                    val fileInfo = mutableMapOf<String, Any>(
                        "url" to url,
                        "public_id" to publicId,
                        "filename" to (file.originalFilename ?: "unknown"),
                        "size" to file.size,
                        "type" to (file.contentType ?: "unknown")
                    )
                    
                    // Si es imagen, agregar variantes
                    if (cloudinaryService.isValidImage(file)) {
                        fileInfo["variants"] = cloudinaryService.getImageVariants(publicId)
                    }
                    
                    uploadedFiles.add(fileInfo)
                } catch (e: Exception) {
                    errors.add("Error con ${file.originalFilename}: ${e.message}")
                }
            }

            val response = mutableMapOf<String, Any>(
                "files" to uploadedFiles,
                "uploaded" to uploadedFiles.size,
                "total" to files.size
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

    @DeleteMapping("/delete")
    fun deleteFile(
        @RequestParam("publicId") publicId: String,
        @RequestParam("resourceType", defaultValue = "image") resourceType: String,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        return try {
            val deleted = cloudinaryService.deleteFile(publicId, resourceType)
            if (deleted) {
                ResponseEntity(mapOf("message" to "Archivo eliminado correctamente"), HttpStatus.OK)
            } else {
                ResponseEntity(mapOf("error" to "No se pudo eliminar el archivo"), HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            ResponseEntity(
                mapOf("error" to "Error al eliminar archivo: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @GetMapping("/transform")
    fun getTransformedImage(
        @RequestParam("publicId") publicId: String,
        @RequestParam("width", required = false) width: Int?,
        @RequestParam("height", required = false) height: Int?,
        @RequestParam("crop", defaultValue = "fill") crop: String,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }

        return try {
            val transformedUrl = cloudinaryService.getTransformedImageUrl(publicId, width, height, crop)
            ResponseEntity(mapOf("url" to transformedUrl), HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(
                mapOf("error" to "Error al transformar imagen: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
} 