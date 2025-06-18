package ar.edu.itba.harmos.dtos.responses

import ar.edu.itba.harmos.models.Report
import ar.edu.itba.harmos.services.CloudinaryService
import java.time.LocalDateTime

data class ReportResponse(
    val id: Long,
    val title: String,
    val date: LocalDateTime,
    val patient: PatientResponse,
    val doctor: AppUserResponse,
    val specialty: SpecialtyResponse,
    val file: FileInfo
) {
    data class FileInfo(
        val url: String,
        val downloadUrl: String,
        val filename: String,
        val publicId: String
    )

    companion object {
        fun singleFromModel(report: Report, cloudinaryService: CloudinaryService): ReportResponse {
            val publicId = cloudinaryService.extractPublicId(report.fileUrl)
            val originalFilename = cloudinaryService.extractFilenameFromUrl(report.fileUrl)
            val downloadUrl = cloudinaryService.getDownloadUrl(publicId, originalFilename, report.fileUrl)
            
            val fileInfo = FileInfo(
                url = report.fileUrl,
                downloadUrl = downloadUrl,
                filename = originalFilename,
                publicId = publicId
            )

            return ReportResponse(
                report.id,
                report.title,
                report.date,
                PatientResponse.singleFromModel(report.patient),
                AppUserResponse.singleFromModel(report.doctor),
                SpecialtyResponse.singleFromModel(report.specialty),
                fileInfo
            )
        }

        fun listFromModel(reports: List<Report>, cloudinaryService: CloudinaryService): List<ReportResponse> {
            return reports.map { singleFromModel(it, cloudinaryService) }
        }
    }
} 