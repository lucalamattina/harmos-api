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
                "transformation", ObjectUtils.asMap(
                    "quality", "auto",
                    "fetch_format", "auto",
                    "width", 1200,
                    "height", 800,
                    "crop", "limit"
                ),
                "overwrite", true,
                "notification_url", ""
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
            val uploadParams = ObjectUtils.asMap(
                "folder", "harmos/$folder",
                "resource_type", "raw", // Para documentos que no son imágenes
                "overwrite", true
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
        val regex = """/v\d+/(.+)\.[^.]+$""".toRegex()
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.get(1) ?: ""
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
        return file.contentType in allowedTypes && file.size <= 10 * 1024 * 1024 // Max 10MB
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
        return file.contentType in allowedTypes && file.size <= 50 * 1024 * 1024 // Max 50MB
    }
} 