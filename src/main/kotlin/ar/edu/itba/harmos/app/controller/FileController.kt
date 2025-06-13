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

        // Validaciones previas
        if (file.isEmpty) {
            return ResponseEntity(mapOf("error" to "El archivo está vacío"), HttpStatus.BAD_REQUEST)
        }
        
        if (file.originalFilename.isNullOrBlank()) {
            return ResponseEntity(mapOf("error" to "El archivo no tiene un nombre válido"), HttpStatus.BAD_REQUEST)
        }

        return try {
            val imageUrl = cloudinaryService.uploadImage(file, folder)
            val publicId = cloudinaryService.extractPublicId(imageUrl)
            val variants = cloudinaryService.getImageVariants(publicId)
            
            ResponseEntity(mapOf(
                "url" to imageUrl,
                "public_id" to publicId,
                "variants" to variants,
                "filename" to file.originalFilename,
                "size" to file.size,
                "contentType" to file.contentType
            ), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf(
                "error" to "Validación fallida: ${e.message}",
                "filename" to file.originalFilename,
                "size" to file.size,
                "contentType" to file.contentType
            ), HttpStatus.BAD_REQUEST)
        } catch (e: RuntimeException) {
            e.printStackTrace()
            ResponseEntity(mapOf(
                "error" to "Error de Cloudinary: ${e.message}",
                "filename" to file.originalFilename,
                "details" to e.cause?.message
            ), HttpStatus.INTERNAL_SERVER_ERROR)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity(mapOf(
                "error" to "Error inesperado: ${e.javaClass.simpleName} - ${e.message}",
                "filename" to file.originalFilename
            ), HttpStatus.INTERNAL_SERVER_ERROR)
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

        // Validaciones previas
        if (file.isEmpty) {
            return ResponseEntity(mapOf("error" to "El archivo está vacío"), HttpStatus.BAD_REQUEST)
        }
        
        if (file.originalFilename.isNullOrBlank()) {
            return ResponseEntity(mapOf("error" to "El archivo no tiene un nombre válido"), HttpStatus.BAD_REQUEST)
        }

        return try {
            val documentUrl = cloudinaryService.uploadDocument(file, folder)
            val publicId = cloudinaryService.extractPublicId(documentUrl)
            
            ResponseEntity(mapOf(
                "url" to documentUrl,
                "download_url" to cloudinaryService.getDownloadUrl(publicId, file.originalFilename),
                "public_id" to publicId,
                "filename" to file.originalFilename,
                "size" to file.size,
                "contentType" to file.contentType
            ), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf(
                "error" to "Validación fallida: ${e.message}",
                "filename" to file.originalFilename,
                "size" to file.size,
                "contentType" to file.contentType
            ), HttpStatus.BAD_REQUEST)
        } catch (e: RuntimeException) {
            e.printStackTrace()
            ResponseEntity(mapOf(
                "error" to "Error de Cloudinary: ${e.message}",
                "filename" to file.originalFilename,
                "details" to e.cause?.message
            ), HttpStatus.INTERNAL_SERVER_ERROR)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity(mapOf(
                "error" to "Error inesperado: ${e.javaClass.simpleName} - ${e.message}",
                "filename" to file.originalFilename
            ), HttpStatus.INTERNAL_SERVER_ERROR)
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
                    // Validaciones previas
                    if (file.isEmpty) {
                        errors.add("Archivo ${file.originalFilename ?: "sin nombre"}: El archivo está vacío")
                        return@forEach
                    }
                    
                    if (file.originalFilename.isNullOrBlank()) {
                        errors.add("Un archivo no tiene nombre válido")
                        return@forEach
                    }
                    
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
                    val originalFilename = file.originalFilename ?: "unknown"
                    val fileInfo = mutableMapOf<String, Any>(
                        "url" to url,
                        "public_id" to publicId,
                        "filename" to originalFilename,
                        "size" to file.size,
                        "type" to (file.contentType ?: "unknown")
                    )
                    
                    // Si es imagen, agregar variantes
                    if (cloudinaryService.isValidImage(file)) {
                        fileInfo["variants"] = cloudinaryService.getImageVariants(publicId)
                    } else {
                        // Si es documento, agregar URL de descarga
                        fileInfo["download_url"] = cloudinaryService.getDownloadUrl(publicId, originalFilename)
                    }
                    
                    uploadedFiles.add(fileInfo)
                } catch (e: IllegalArgumentException) {
                    errors.add("Archivo ${file.originalFilename}: Validación fallida - ${e.message}")
                } catch (e: RuntimeException) {
                    errors.add("Archivo ${file.originalFilename}: Error de Cloudinary - ${e.message}")
                    e.printStackTrace()
                } catch (e: Exception) {
                    errors.add("Archivo ${file.originalFilename}: Error inesperado - ${e.javaClass.simpleName}: ${e.message}")
                    e.printStackTrace()
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

    @GetMapping("/test/connection")
    fun testCloudinaryConnection(@CurrentUser appUser: AppUser?): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }
        
        val connectionTest = cloudinaryService.testConnection()
        return ResponseEntity(connectionTest, HttpStatus.OK)
    }
    
    @GetMapping("/test/urls")
    fun testUrlGeneration(
        @RequestParam("publicId") publicId: String,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }
        
        val urlTest = cloudinaryService.testUrlGeneration(publicId)
        return ResponseEntity(urlTest, HttpStatus.OK)
    }

    @GetMapping("/test/signed")
    fun testSignedUrls(
        @RequestParam("publicId") publicId: String,
        @RequestParam("filename") filename: String,
        @RequestParam("resourceType", defaultValue = "raw") resourceType: String,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }
        
        try {
            val signedUrl = cloudinaryService.getSignedUrl(publicId, resourceType)
            val signedDownloadUrl = cloudinaryService.getSignedDownloadUrl(publicId, filename, resourceType)
            
            val response = mapOf(
                "public_id" to publicId,
                "filename" to filename,
                "resource_type" to resourceType,
                "signed_url" to signedUrl,
                "signed_download_url" to signedDownloadUrl
            )
            
            return ResponseEntity(response, HttpStatus.OK)
        } catch (e: Exception) {
            return ResponseEntity(
                mapOf("error" to "Error generando URLs firmadas: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @GetMapping("/test/preview")
    fun testDocumentPreview(
        @RequestParam("publicId") publicId: String,
        @RequestParam("filename") filename: String,
        @RequestParam("resourceType", defaultValue = "raw") resourceType: String,
        @CurrentUser appUser: AppUser?
    ): ResponseEntity<Any> {
        if (appUser == null) {
            return ResponseEntity(mapOf("error" to "Usuario no autenticado"), HttpStatus.UNAUTHORIZED)
        }
        
        try {
            val documentType = cloudinaryService.getDocumentType(filename)
            val response = mutableMapOf<String, Any>(
                "public_id" to publicId,
                "filename" to filename,
                "document_type" to documentType,
                "resource_type" to resourceType
            )
            
            // Generar URLs de preview si es un documento soportado
            if (documentType in listOf("pdf", "word", "excel", "powerpoint")) {
                response["preview_url"] = cloudinaryService.getDocumentPreviewUrl(publicId, null)
                response["thumbnail_url"] = cloudinaryService.getDocumentThumbnail(publicId, null)
                response["pages"] = cloudinaryService.getDocumentPages(publicId, null, 5)
            }
            
            return ResponseEntity(response, HttpStatus.OK)
        } catch (e: Exception) {
            return ResponseEntity(
                mapOf("error" to "Error generando preview: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
} 