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
            
            // Todos los documentos van como RAW para evitar problemas de acceso
            val uploadParams = ObjectUtils.asMap(
                "public_id", "harmos/$folder/${sanitizedBasename}_$timestamp.$fileExtension",
                "resource_type", "raw",
                "format", fileExtension,
                "overwrite", false,
                "use_filename", true,
                "unique_filename", false,
                "access_mode", "public", // Asegurar acceso público
                "type", "upload" // Tipo de upload público
            )
            
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
        // El public_id tiene el formato: harmos/folder/filename_timestamp.ext
        val filename = publicId.substringAfterLast("/")
        
        // Buscar el patrón filename_timestamp.ext y extraer filename.ext
        val timestampRegex = """^(.+)_\d+(\..+)$""".toRegex()
        val matchResult = timestampRegex.find(filename)
        
        return if (matchResult != null) {
            val baseName = matchResult.groupValues[1] // Parte antes del timestamp
            val extension = matchResult.groupValues[2] // Extensión
            "$baseName$extension"
        } else {
            filename // Si no hay timestamp, devolver tal como está
        }
    }
    
    /**
     * Extrae el nombre de archivo original desde URL de Cloudinary
     */
    fun extractFilenameFromUrl(url: String): String {
        val publicId = extractPublicId(url)
        return extractFilenameFromPublicId(publicId)
    }

    /**
     * Genera una URL de descarga que preserva el nombre original del archivo
     */
    fun getDownloadUrl(publicId: String, originalFilename: String? = null, fileUrl: String? = null): String {
        val filename = originalFilename ?: extractFilenameFromPublicId(publicId)
        
        // Detectar si el archivo está en image o raw upload basándose en la URL original
        val resourcePath = when {
            fileUrl?.contains("/image/upload/") == true -> "image"
            fileUrl?.contains("/raw/upload/") == true -> "raw"
            else -> "raw" // Por defecto raw para nuevos archivos
        }
        
        // Generar URL de descarga con attachment flag (formato correcto)
        return "https://res.cloudinary.com/$cloudName/$resourcePath/upload/fl_attachment/$publicId?dl=$filename"
    }
    
    /**
     * Genera una URL de visualización directa para documentos
     */
    fun getDirectUrl(publicId: String, fileUrl: String? = null): String {
        // Detectar si el archivo está en image o raw upload basándose en la URL original
        val resourcePath = when {
            fileUrl?.contains("/image/upload/") == true -> "image"
            fileUrl?.contains("/raw/upload/") == true -> "raw"
            else -> "raw" // Por defecto raw para nuevos archivos
        }
        
        // Usar el método oficial de Cloudinary para generar URLs
        return cloudinary.url()
            .resourceType(resourcePath)
            .secure(true)
            .generate(publicId) ?: ""
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
        val baseUrl = cloudinary.url().generate(publicId) ?: ""
        
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
            "original" to (cloudinary.url().generate(publicId) ?: "")
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

    /**
     * Función de diagnóstico para probar conectividad con Cloudinary
     */
    fun testConnection(): Map<String, Any> {
        return try {
            // Test básico de conectividad usando recursos existentes
            val resources = cloudinary.api().resources(ObjectUtils.asMap("max_results", 1))
            
            // Información de la cuenta
            mapOf(
                "cloud_name" to cloudName,
                "api_key_prefix" to apiKey.take(4) + "****",
                "connection_status" to "OK",
                "resources_found" to ((resources["resources"] as? List<*>)?.size ?: 0)
            )
        } catch (e: Exception) {
            mapOf(
                "cloud_name" to cloudName,
                "api_key_prefix" to apiKey.take(4) + "****",
                "connection_status" to "ERROR",
                "error" to (e.message ?: "Unknown error"),
                "error_type" to e.javaClass.simpleName
            )
        }
    }
    
    /**
     * Función para probar URLs generadas vs URLs directas de Cloudinary
     */
    fun testUrlGeneration(publicId: String): Map<String, Any> {
        return try {
            val manualRawUrl = "https://res.cloudinary.com/$cloudName/raw/upload/$publicId"
            val manualImageUrl = "https://res.cloudinary.com/$cloudName/image/upload/$publicId"
            val sdkGeneratedUrl = cloudinary.url().resourceType("raw").secure(true).generate(publicId) ?: ""
            val sdkImageUrl = cloudinary.url().resourceType("image").secure(true).generate(publicId) ?: ""
            
            mapOf(
                "public_id" to publicId,
                "manual_raw_url" to manualRawUrl,
                "manual_image_url" to manualImageUrl,
                "sdk_raw_url" to sdkGeneratedUrl,
                "sdk_image_url" to sdkImageUrl,
                "cloud_name" to cloudName
            )
        } catch (e: Exception) {
            mapOf(
                "error" to (e.message ?: "Unknown error"),
                "error_type" to e.javaClass.simpleName
            )
        }
    }

    /**
     * Genera URLs firmadas que evitan restricciones de seguridad
     */
    fun getSignedUrl(publicId: String, resourceType: String = "raw"): String {
        return cloudinary.url()
            .resourceType(resourceType)
            .secure(true)
            .signed(true)
            .generate(publicId) ?: ""
    }
    
    /**
     * Genera URLs de descarga firmadas
     */
    fun getSignedDownloadUrl(publicId: String, filename: String, resourceType: String = "raw"): String {
        val baseUrl = cloudinary.url()
            .resourceType(resourceType)
            .secure(true)
            .signed(true)
            .generate(publicId) ?: ""
        return "$baseUrl?fl_attachment=$filename"
    }

    /**
     * Genera URL de preview para documentos (Word, PDF, etc.)
     */
    fun getDocumentPreviewUrl(publicId: String, fileUrl: String? = null, page: Int = 1): String {
        val resourcePath = when {
            fileUrl?.contains("/image/upload/") == true -> "image"
            fileUrl?.contains("/raw/upload/") == true -> "raw"
            else -> "raw"
        }
        
        // Para documentos, Cloudinary puede generar previews como imagen
        return "https://res.cloudinary.com/$cloudName/$resourcePath/upload/f_jpg,pg_$page,w_800,h_600,c_fit/$publicId"
    }
    
    /**
     * Genera múltiples páginas de preview para documentos
     */
    fun getDocumentPages(publicId: String, fileUrl: String? = null, maxPages: Int = 5): List<Map<String, Any>> {
        val pages = mutableListOf<Map<String, Any>>()
        
        for (page in 1..maxPages) {
            try {
                val previewUrl = getDocumentPreviewUrl(publicId, fileUrl, page)
                pages.add(mapOf(
                    "page" to page,
                    "preview_url" to previewUrl,
                    "thumbnail_url" to getDocumentThumbnail(publicId, fileUrl, page)
                ))
            } catch (e: Exception) {
                // Si falla una página, parar
                break
            }
        }
        
        return pages
    }
    
    /**
     * Genera thumbnail pequeño de una página específica
     */
    fun getDocumentThumbnail(publicId: String, fileUrl: String? = null, page: Int = 1): String {
        val resourcePath = when {
            fileUrl?.contains("/image/upload/") == true -> "image"
            fileUrl?.contains("/raw/upload/") == true -> "raw"
            else -> "raw"
        }
        
        return "https://res.cloudinary.com/$cloudName/$resourcePath/upload/f_jpg,pg_$page,w_200,h_150,c_fit/$publicId"
    }
    
    /**
     * Detecta el tipo de documento basado en la extensión
     */
    fun getDocumentType(filename: String): String {
        val extension = filename.substringAfterLast(".", "").lowercase()
        return when (extension) {
            "pdf" -> "pdf"
            "doc", "docx" -> "word"
            "xls", "xlsx" -> "excel"
            "ppt", "pptx" -> "powerpoint"
            "txt" -> "text"
            else -> "document"
        }
    }
} 