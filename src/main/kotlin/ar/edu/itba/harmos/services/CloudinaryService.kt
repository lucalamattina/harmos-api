package ar.edu.itba.harmos.services

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@Service
class CloudinaryService(
    @Value("\${cloudinary.cloud-name}") private val cloudName: String,
    @Value("\${cloudinary.api-key}") private val apiKey: String,
    @Value("\${cloudinary.api-secret}") private val apiSecret: String
) {
    
    private val cloudinary: Cloudinary by lazy {
        Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        ))
    }
    
    /**
     * Sube una imagen a Cloudinary con transformaciones automáticas
     * @param file archivo de imagen a subir
     * @param folder carpeta donde guardar (ej: "announcements", "reports")
     * @return URL de la imagen subida
     */
    fun uploadImage(file: MultipartFile, folder: String): String {
        if (!isValidImage(file)) {
            throw IllegalArgumentException("Archivo no válido. Solo se permiten imágenes menores a 10MB")
        }
        
        try {
            val uploadParams = ObjectUtils.asMap(
                "folder", "harmos/$folder",
                "resource_type", "image",
                "quality", "auto",
                "fetch_format", "auto",
                "width", 1200,
                "height", 800,
                "crop", "limit",
                "overwrite", true
            )
            
            val result = cloudinary.uploader().upload(file.bytes, uploadParams)
            return result["secure_url"] as String
        } catch (e: IOException) {
            throw RuntimeException("Error uploading image to Cloudinary", e)
        }
    }
    
    /**
     * Sube un documento a Cloudinary
     * @param file archivo de documento a subir
     * @param folder carpeta donde guardar
     * @return URL del documento subido
     */
    fun uploadDocument(file: MultipartFile, folder: String): String {
        if (!isValidDocument(file)) {
            throw IllegalArgumentException("Archivo no válido. Solo se permiten documentos menores a 50MB")
        }
        
        try {
            // Generar un public_id que incluya el nombre original del archivo
            val originalFilename = file.originalFilename ?: "unknown"
            val fileExtension = originalFilename.substringAfterLast(".", "").lowercase()
            val baseFilename = originalFilename.substringBeforeLast(".")
            val sanitizedBasename = baseFilename.replace(Regex("[^a-zA-Z0-9._-]"), "")
            val timestamp = System.currentTimeMillis()
            
            // Usar estrategia diferente según el tipo de archivo
            val uploadParams = when (fileExtension) {
                "pdf" -> {
                    // Para PDFs, usar resource_type auto para mejor detección
                    ObjectUtils.asMap(
                        "public_id", "harmos/$folder/${sanitizedBasename}_$timestamp",
                        "resource_type", "auto",
                        "format", "pdf",
                        "overwrite", false,
                        "use_filename", true,
                        "unique_filename", false
                    )
                }
                "doc", "docx" -> {
                    // Para archivos de Word, usar resource_type auto para mejor detección
                    ObjectUtils.asMap(
                        "public_id", "harmos/$folder/${sanitizedBasename}_$timestamp",
                        "resource_type", "auto",
                        "format", fileExtension,
                        "overwrite", false,
                        "use_filename", true,
                        "unique_filename", false
                    )
                }
                else -> {
                    // Para otros documentos, mantener como raw pero con más información
                    ObjectUtils.asMap(
                        "public_id", "harmos/$folder/${sanitizedBasename}_$timestamp.$fileExtension",
                        "resource_type", "raw",
                        "format", fileExtension,
                        "overwrite", false,
                        "use_filename", true,
                        "unique_filename", false
                    )
                }
            }
            
            val result = cloudinary.uploader().upload(file.bytes, uploadParams)
            return result["secure_url"] as String
        } catch (e: IOException) {
            throw RuntimeException("Error uploading document to Cloudinary", e)
        }
    }
    
    /**
     * Sube múltiples archivos
     */
    fun uploadFiles(files: List<MultipartFile>, folder: String, type: String = "document"): List<String> {
        return files.map { file ->
            when (type) {
                "image" -> uploadImage(file, folder)
                "document" -> uploadDocument(file, folder)
                else -> if (isValidImage(file)) uploadImage(file, folder) else uploadDocument(file, folder)
            }
        }
    }
    
    /**
     * Elimina un archivo de Cloudinary
     * @param publicId ID público del archivo en Cloudinary
     * @param resourceType tipo de recurso ("image" o "raw")
     */
    fun deleteFile(publicId: String, resourceType: String = "image"): Boolean {
        return try {
            val result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType))
            result["result"] == "ok"
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Extrae el public_id de una URL de Cloudinary
     */
    fun extractPublicId(url: String): String {
        // Para URLs con versión: https://res.cloudinary.com/cloud/resource_type/upload/v1234567890/path/file.ext
        val regexWithVersion = """/v\d+/(.+)$""".toRegex()
        val matchWithVersion = regexWithVersion.find(url)
        if (matchWithVersion != null) {
            return matchWithVersion.groupValues[1]
        }
        
        // Para URLs sin versión: https://res.cloudinary.com/cloud/resource_type/upload/path/file.ext
        val regexWithoutVersion = """/upload/(.+)$""".toRegex()
        val matchWithoutVersion = regexWithoutVersion.find(url)
        return matchWithoutVersion?.groupValues?.get(1) ?: ""
    }

    /**
     * Extrae el nombre de archivo original desde el public_id
     */
    fun extractFilenameFromPublicId(publicId: String): String {
        // El public_id puede tener diferentes formatos:
        // 1. harmos/folder/filename_timestamp (para PDFs/Word con resource_type auto)
        // 2. harmos/folder/filename_timestamp.ext (para otros documentos)
        val filename = publicId.substringAfterLast("/")
        
        // Buscar el patrón filename_timestamp o filename_timestamp.ext
        val timestampRegex = """^(.+)_\d+(\..+)?$""".toRegex()
        val matchResult = timestampRegex.find(filename)
        
        return if (matchResult != null) {
            val baseName = matchResult.groupValues[1] // Parte antes del timestamp
            val extension = matchResult.groupValues[2] // Extensión si existe
            
            // Si no hay extensión en el public_id (archivos con resource_type "auto")
            if (extension.isEmpty()) {
                baseName // Devolver solo el nombre base, la extensión se inferirá del contexto de la URL
            } else {
                "$baseName$extension"
            }
        } else {
            filename // Si no hay timestamp, devolver tal como está
        }
    }
    
    /**
     * Extrae el nombre de archivo original y detecta el tipo desde URL de Cloudinary
     */
    fun extractFilenameFromUrl(url: String): String {
        val publicId = extractPublicId(url)
        val baseFilename = extractFilenameFromPublicId(publicId)
        
        // Si el archivo no tiene extensión, intentar detectarla desde la URL
        if (!baseFilename.contains(".")) {
            return when {
                url.contains("/image/upload/") -> "$baseFilename.jpg" // Probablemente imagen
                url.contains("/video/upload/") -> "$baseFilename.mp4" // Probablemente video
                url.contains("/raw/upload/") -> {
                    // Para archivos raw, intentar detectar desde el contenido de la URL
                    when {
                        url.contains("pdf") -> "$baseFilename.pdf"
                        url.contains("doc") -> "$baseFilename.docx"
                        url.contains("xls") -> "$baseFilename.xlsx"
                        url.contains("ppt") -> "$baseFilename.pptx"
                        else -> "$baseFilename.pdf" // Por defecto PDF
                    }
                }
                else -> "$baseFilename.pdf" // Por defecto PDF
            }
        }
        
        return baseFilename
    }

    /**
     * Genera una URL de descarga que preserva el nombre original del archivo
     */
    fun getDownloadUrl(publicId: String, originalFilename: String? = null): String {
        val filename = originalFilename ?: extractFilenameFromPublicId(publicId)
        return "https://res.cloudinary.com/$cloudName/raw/upload/fl_attachment:$filename/$publicId"
    }
    
    /**
     * Obtiene una URL transformada de una imagen
     * @param publicId ID público de la imagen
     * @param width ancho deseado
     * @param height alto deseado
     * @param crop tipo de recorte
     */
    fun getTransformedImageUrl(publicId: String, width: Int? = null, height: Int? = null, crop: String = "fill"): String {
        // Construir URL manual con transformaciones si es necesario
        val baseUrl = cloudinary.url().generate(publicId)
        
        if (width == null && height == null) {
            return baseUrl
        }
        
        // Para transformaciones complejas, usar la URL base de Cloudinary
        var transformParams = "q_auto,f_auto,c_$crop"
        width?.let { transformParams += ",w_$it" }
        height?.let { transformParams += ",h_$it" }
        
        // Extraer el public_id y construir URL con transformaciones
        val cloudUrl = "https://res.cloudinary.com/$cloudName/image/upload/$transformParams/$publicId"
        return cloudUrl
    }
    
    /**
     * Obtiene URLs de imagen en diferentes tamaños (thumbnails)
     */
    fun getImageVariants(publicId: String): Map<String, String> {
        return mapOf(
            "thumbnail" to getTransformedImageUrl(publicId, 150, 150, "fill"),
            "small" to getTransformedImageUrl(publicId, 400, 300, "fill"),
            "medium" to getTransformedImageUrl(publicId, 800, 600, "fill"),
            "large" to getTransformedImageUrl(publicId, 1200, 900, "fill"),
            "original" to cloudinary.url().generate(publicId)
        )
    }
    
    /**
     * Verifica si un archivo es una imagen válida
     */
    fun isValidImage(file: MultipartFile): Boolean {
        val allowedTypes = listOf("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp")
        return !file.isEmpty && 
               file.contentType in allowedTypes && 
               file.size <= 10 * 1024 * 1024 && // Max 10MB
               !file.originalFilename.isNullOrBlank()
    }
    
    /**
     * Verifica si un archivo es un documento válido
     */
    fun isValidDocument(file: MultipartFile): Boolean {
        val allowedTypes = listOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        )
        
        // Validar por content-type
        val contentTypeValid = file.contentType in allowedTypes
        
        // Validar por extensión como fallback
        val filename = file.originalFilename?.lowercase() ?: ""
        val allowedExtensions = listOf(".pdf", ".doc", ".docx", ".txt", ".xls", ".xlsx", ".ppt", ".pptx")
        val extensionValid = allowedExtensions.any { filename.endsWith(it) }
        
        return !file.isEmpty && 
               (contentTypeValid || extensionValid) && // Aceptar si content-type O extensión son válidos
               file.size <= 50 * 1024 * 1024 && // Max 50MB
               !file.originalFilename.isNullOrBlank()
    }
} 