package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.models.Report
import ar.edu.itba.harmos.persistence.ReportRepository
import org.springframework.stereotype.Service

@Service
class ReportService(
    private val reportRepository: ReportRepository
) {
    
    fun getReportById(id: Long): Report? {
        val opt = reportRepository.findById(id)
        return if (opt.isPresent) {
            opt.get()
        } else {
            null
        }
    }
    
    fun updateReportFiles(report: Report): Report {
        return reportRepository.save(report)
    }
    
    fun getAllReports(): List<Report> {
        return reportRepository.findAll().toList()
    }
    
    fun getReportsByPatientId(patientId: Long): List<Report> {
        return reportRepository.findByPatientId(patientId)
    }
    
    fun getReportsByDoctorId(doctorId: Long): List<Report> {
        return reportRepository.findByDoctorId(doctorId)
    }
} 