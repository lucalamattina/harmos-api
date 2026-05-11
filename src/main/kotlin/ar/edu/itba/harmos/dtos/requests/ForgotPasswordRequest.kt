package ar.edu.itba.harmos.dtos.requests

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class ForgotPasswordRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be a valid email address")
    val email: String = ""
) {
    fun isValid(): Boolean {
        val trimmedEmail = email.trim()
        return trimmedEmail.isNotBlank() && isValidEmail(trimmedEmail)
    }

    fun getValidationError(): String? {
        val trimmedEmail = email.trim()
        return when {
            email.isBlank() -> "El email es obligatorio"
            trimmedEmail.isBlank() -> "El email no puede estar vacío"
            trimmedEmail.contains("=") -> "El formato del email no es válido. Formato correcto: usuario@dominio.com"
            !trimmedEmail.contains("@") -> "El email debe contener el símbolo @"
            !trimmedEmail.contains(".") -> "El email debe contener un dominio válido (ej: .com, .org)"
            trimmedEmail.startsWith("@") -> "El email no puede empezar con @"
            trimmedEmail.endsWith("@") -> "El email debe tener un dominio después del @"
            else -> null
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }
}
