package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.CreateReportRequest
import ar.edu.itba.harmos.models.AppUser
import ar.edu.itba.harmos.models.Patient
import ar.edu.itba.harmos.models.PatientStatus
import ar.edu.itba.harmos.models.Report
import ar.edu.itba.harmos.models.Specialty
import ar.edu.itba.harmos.security.resolvers.CurrentUserArgumentResolver
import ar.edu.itba.harmos.services.AppUserService
import ar.edu.itba.harmos.services.CloudinaryService
import ar.edu.itba.harmos.services.ReportService
import io.jsonwebtoken.Jwts
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doReturn
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.multipart.MultipartFile
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

/**
 * Kotlin-safe any() — avoids call-site NPE when Mockito's any() returns null for non-null params.
 * castNull() is needed because Kotlin 1.6 rejects `null as T` at call sites but suppresses it here.
 */
@Suppress("UNCHECKED_CAST")
private fun <T> castNull(): T = null as T
private fun <T : Any> anyK(): T = any<T>() ?: castNull()

@ExtendWith(MockitoExtension::class)
class ReportControllerTest {

    @Mock private lateinit var reportService: ReportService
    @Mock private lateinit var cloudinaryService: CloudinaryService
    @Mock private lateinit var appUserService: AppUserService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val controller = ReportController(reportService, cloudinaryService)
        val resolver = CurrentUserArgumentResolver(appUserService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(resolver)
            .build()
    }

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    // ─────────────────────────── helpers ────────────────────────────────────

    private fun createUser() = AppUser(
        email = "doc@test.com",
        password = "x",
        firstName = "Doc",
        lastName = "Tor",
        phone = "0",
        roles = mutableSetOf(),
        id = 1L
    )

    private fun createReport(user: AppUser): Report {
        val specialty = Specialty(name = "Cardiology", id = 1L)
        val patient = Patient(
            firstName = "Jane",
            lastName = "Doe",
            phone = "0",
            status = PatientStatus.PENDING,
            doctors = mutableListOf(user),
            reports = emptyList(),
            id = 1L
        )
        return Report(
            title = "Test Report",
            patient = patient,
            doctor = user,
            specialty = specialty,
            fileUrl = "http://cloudinary.example.com/reports/report.pdf",
            date = LocalDateTime.now(),
            id = 1L
        )
    }

    /** Puts a JWT Claims principal into the security context and stubs appUserService. */
    private fun setCurrentUser(user: AppUser) {
        val claims = Jwts.claims().setSubject(user.id.toString())
        val auth = UsernamePasswordAuthenticationToken(claims, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
        `when`(appUserService.getAppUserById(user.id)).thenReturn(user)
    }

    // ─────────────────── Group 1 — Authentication ───────────────────────────

    @Test
    fun `createReport returns 401 when no user is authenticated`() {
        // No security context set → CurrentUserArgumentResolver returns null → controller returns 401
        mockMvc.perform(
            multipart("/reports")
                .file(MockMultipartFile("file", "report.pdf", "application/pdf", ByteArray(2048)))
                .param("title", "Test")
                .param("patientId", "1")
                .param("specialtyId", "1")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("Usuario no autenticado"))
    }

    // ─────────────────── Group 2 — File validation (validateReportFile) ─────

    /**
     * When originalFilename is blank (Spring normalises null to "") validateReportFile
     * returns a failure → controller places the error in the "file" field-error map → 400.
     */
    @Test
    fun `createReport returns 400 when file has a blank filename`() {
        setCurrentUser(createUser())

        mockMvc.perform(
            multipart("/reports")
                .file(MockMultipartFile("file", "", "application/pdf", ByteArray(2048)))
                .param("title", "Test")
                .param("patientId", "1")
                .param("specialtyId", "1")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.file").value("El archivo debe tener un nombre válido"))
    }

    /**
     * Files larger than 25 MB must be rejected regardless of type.
     * 26 * 1024 * 1024 = 27 262 976 bytes → exceeds the 25 MB limit.
     */
    @Test
    fun `createReport returns 400 when file exceeds 25MB`() {
        setCurrentUser(createUser())

        mockMvc.perform(
            multipart("/reports")
                .file(MockMultipartFile("file", "report.pdf", "application/pdf", ByteArray(26 * 1024 * 1024)))
                .param("title", "Test")
                .param("patientId", "1")
                .param("specialtyId", "1")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.file").value("El archivo es muy grande (máximo 25MB)"))
    }

    /**
     * Files smaller than 1 KB (1024 bytes) must be rejected.
     * 512 bytes < 1024 bytes.
     */
    @Test
    fun `createReport returns 400 when file is smaller than 1KB`() {
        setCurrentUser(createUser())

        mockMvc.perform(
            multipart("/reports")
                .file(MockMultipartFile("file", "report.pdf", "application/pdf", ByteArray(512)))
                .param("title", "Test")
                .param("patientId", "1")
                .param("specialtyId", "1")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.file").value("El archivo es muy pequeño (mínimo 1KB)"))
    }

    /**
     * AND-check: MIME type matches a document family but the extension does NOT.
     * "malicious.exe" + "application/pdf" → docExtMatch=false, docMimeMatch=true → failure.
     *
     * ⚠️ Potential bug: The AND-check correctly prevents MIME-only or extension-only spoofing.
     * However, the two families (document and image) are evaluated independently, so a file
     * with a document MIME and an image extension (or vice versa) also falls through to the
     * else branch and is correctly rejected. The logic is sound for the defined families.
     */
    @Test
    fun `createReport returns 400 when MIME matches document but extension does not`() {
        setCurrentUser(createUser())

        mockMvc.perform(
            multipart("/reports")
                .file(MockMultipartFile("file", "malicious.exe", "application/pdf", ByteArray(2048)))
                .param("title", "Test")
                .param("patientId", "1")
                .param("specialtyId", "1")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.file").exists())
    }

    /**
     * AND-check: extension matches a document family but the MIME type does NOT.
     * "report.pdf" + "application/octet-stream" → docExtMatch=true, docMimeMatch=false → failure.
     */
    @Test
    fun `createReport returns 400 when extension matches document but MIME does not`() {
        setCurrentUser(createUser())

        mockMvc.perform(
            multipart("/reports")
                .file(MockMultipartFile("file", "report.pdf", "application/octet-stream", ByteArray(2048)))
                .param("title", "Test")
                .param("patientId", "1")
                .param("specialtyId", "1")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.file").exists())
    }

    /**
     * An empty file (size == 0) is caught by the `file.isEmpty` guard in createReport
     * BEFORE validateReportFile is ever called, and produces a distinct error message.
     *
     * ⚠️ Potential bug: The "empty file" check and the "< 1 KB" check use different
     * code paths and produce different error messages. A 0-byte file lands on
     * "El archivo es obligatorio para crear un reporte" (isEmpty guard), while a
     * 512-byte file lands on "El archivo es muy pequeño (mínimo 1KB)" (validateReportFile).
     * Both are rejected with 400, which is correct, but the inconsistency in error messages
     * could confuse clients expecting a single "too small" message.
     */
    @Test
    fun `createReport returns 400 when file is empty (size = 0)`() {
        setCurrentUser(createUser())

        mockMvc.perform(
            multipart("/reports")
                .file(MockMultipartFile("file", "report.pdf", "application/pdf", ByteArray(0)))
                .param("title", "Test")
                .param("patientId", "1")
                .param("specialtyId", "1")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.file").value("El archivo es obligatorio para crear un reporte"))
    }

    /**
     * Happy path: valid PDF file (extension + MIME both pass the AND-check, size in range).
     * Stubs cloudinaryService.uploadDocument, cloudinaryService helper methods, and
     * reportService.createReportWithFile to simulate a successful creation.
     *
     * ⚠️ Potential bug: validateReportFile() is called TWICE on the happy path — once during
     * the field-error validation block (line ~133) and again immediately after (line ~145)
     * to extract the isImage flag. The second call is redundant and adds unnecessary overhead.
     * The result of the first call should be stored and reused.
     */
    @Test
    fun `createReport returns 201 for valid PDF with matching extension and MIME`() {
        val user = createUser()
        setCurrentUser(user)

        val report = createReport(user)
        val fileUrl = "http://cloudinary.example.com/reports/report.pdf"

        // Use doReturn + anyK() to avoid Kotlin call-site NPE when Mockito matchers return null
        doReturn(fileUrl).`when`(cloudinaryService).uploadDocument(anyK<MultipartFile>(), anyString())
        doReturn(report).`when`(reportService).createReportWithFile(
            anyK<CreateReportRequest>(),
            anyK<AppUser>(),
            anyString()
        )

        // Stubs needed by ReportResponse.singleFromModel
        doReturn("reports/report").`when`(cloudinaryService).extractPublicId(anyString())
        doReturn("report.pdf").`when`(cloudinaryService).extractFilenameFromUrl(anyString())
        doReturn("http://cloudinary.example.com/reports/report.pdf?download=1")
            .`when`(cloudinaryService).getDownloadUrl(anyString(), anyString(), anyString())

        mockMvc.perform(
            multipart("/reports")
                .file(MockMultipartFile("file", "report.pdf", "application/pdf", ByteArray(2048)))
                .param("title", "Test Report")
                .param("patientId", "1")
                .param("specialtyId", "1")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(report.id))
            .andExpect(jsonPath("$.title").value(report.title))
    }
}
