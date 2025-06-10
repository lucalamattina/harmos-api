# Configuración de Cloudinary para Harmos

## Pasos para configurar Cloudinary

### 1. Crear cuenta en Cloudinary
1. Ve a [Cloudinary](https://cloudinary.com/) y crea una cuenta gratuita
2. Una vez registrado, ve al **Dashboard**
3. Anota los siguientes valores:
   - **Cloud Name**
   - **API Key**  
   - **API Secret**

### 2. Configurar variables de entorno
Configura las siguientes variables de entorno en tu sistema:

```bash
export CLOUDINARY_CLOUD_NAME=tu-cloud-name
export CLOUDINARY_API_KEY=tu-api-key
export CLOUDINARY_API_SECRET=tu-api-secret
```

O para desarrollo local, puedes crear un archivo `.env` en la raíz del proyecto:

```
CLOUDINARY_CLOUD_NAME=tu-cloud-name
CLOUDINARY_API_KEY=tu-api-key
CLOUDINARY_API_SECRET=tu-api-secret
```

### 3. Configuración en application.yml (Desarrollo)
Para desarrollo, también puedes configurar directamente en `application.yml`:

```yaml
cloudinary:
  cloud-name: tu-cloud-name
  api-key: tu-api-key
  api-secret: tu-api-secret
```

⚠️ **IMPORTANTE**: Nunca commitees las credenciales al repositorio. Usa variables de entorno en producción.

### 4. Configurar organización de archivos
Cloudinary organizará automáticamente los archivos en la siguiente estructura:

```
harmos/
├── announcements/         # Imágenes y archivos de anuncios
└── reports/               # Imágenes médicas y documentos de reportes
```

### 5. Configuración de transformaciones automáticas
El servicio está configurado para aplicar las siguientes optimizaciones automáticamente:

**Para imágenes:**
- Calidad automática (compresión inteligente)
- Formato automático (WebP en navegadores compatibles)
- Redimensionamiento máximo: 1200x800px
- Generación de thumbnails automática

**Para documentos:**
- Almacenamiento como recursos "raw"
- Mantenimiento del formato original

## Endpoints disponibles

### Subir archivos generales
- `POST /files/upload/image` - Subir imagen con optimizaciones
- `POST /files/upload/document` - Subir documento
- `POST /files/upload/multiple` - Subir múltiples archivos
- `DELETE /files/delete` - Eliminar archivo por public_id
- `GET /files/transform` - Obtener URL transformada de imagen

### Manejo de archivos en anuncios
- `POST /announcements/{id}/files` - Subir archivos a un anuncio
- `DELETE /announcements/{id}/files` - Eliminar archivo de un anuncio

### Manejo de archivos en reportes médicos
- `POST /reports/{id}/files` - Subir archivos médicos a un reporte
- `DELETE /reports/{id}/files` - Eliminar archivo de un reporte
- `GET /reports/{id}/files` - Obtener archivos de un reporte con thumbnails

## Ejemplos de uso

### Subir imagen con transformaciones
```bash
curl -X POST \
  http://localhost:8080/files/upload/image \
  -H 'Authorization: Bearer tu-token' \
  -F 'file=@imagen.jpg' \
  -F 'folder=announcements'
```

**Respuesta:**
```json
{
  "url": "https://res.cloudinary.com/tu-cloud/image/upload/v1234567890/harmos/announcements/imagen.jpg",
  "public_id": "harmos/announcements/imagen",
  "variants": {
    "thumbnail": "https://res.cloudinary.com/tu-cloud/image/upload/c_fill,h_150,w_150/harmos/announcements/imagen.jpg",
    "small": "https://res.cloudinary.com/tu-cloud/image/upload/c_fill,h_300,w_400/harmos/announcements/imagen.jpg",
    "medium": "https://res.cloudinary.com/tu-cloud/image/upload/c_fill,h_600,w_800/harmos/announcements/imagen.jpg",
    "large": "https://res.cloudinary.com/tu-cloud/image/upload/c_fill,h_900,w_1200/harmos/announcements/imagen.jpg",
    "original": "https://res.cloudinary.com/tu-cloud/image/upload/harmos/announcements/imagen.jpg"
  }
}
```

### Subir archivos a un anuncio
```bash
curl -X POST \
  http://localhost:8080/announcements/1/files \
  -H 'Authorization: Bearer tu-token' \
  -F 'images=@foto1.jpg' \
  -F 'images=@foto2.png' \
  -F 'files=@documento.pdf'
```

### Obtener imagen transformada
```bash
curl -X GET \
  "http://localhost:8080/files/transform?publicId=harmos/announcements/imagen&width=300&height=200&crop=fill" \
  -H 'Authorization: Bearer tu-token'
```

## Límites y características

### Límites de archivos
- **Imágenes**: 10MB máximo
- **Documentos**: 50MB máximo

### Formatos soportados

**Imágenes:**
- JPEG, PNG, GIF, WebP, BMP

**Documentos:**
- PDF, Word (.doc, .docx), Excel (.xls, .xlsx), PowerPoint (.ppt, .pptx), Texto plano

### Transformaciones disponibles
- Redimensionamiento inteligente
- Compresión automática
- Conversión de formato automática
- Generación de thumbnails
- Recorte inteligente

### Ventajas de Cloudinary
✅ **CDN global** - Entrega rápida en todo el mundo
✅ **Optimización automática** - Reduce tamaño sin perder calidad  
✅ **Transformaciones en tiempo real** - URLs dinámicas para diferentes tamaños
✅ **Backup automático** - Tus archivos están seguros
✅ **Analytics** - Estadísticas de uso de imágenes
✅ **Tier gratuito generoso** - 25GB de almacenamiento y 25GB de bandwidth mensual

## Seguridad
- Solo usuarios autenticados pueden subir archivos
- Control de acceso granular para reportes médicos
- Validación de tipos de archivo
- URLs firmadas para contenido privado (si se necesita en el futuro)

## Monitoreo
Puedes monitorear el uso en el Dashboard de Cloudinary:
- Almacenamiento utilizado
- Bandwidth consumido
- Transformaciones realizadas
- Archivos más accedidos 