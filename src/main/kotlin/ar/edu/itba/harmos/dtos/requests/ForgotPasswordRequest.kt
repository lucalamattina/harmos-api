package ar.edu.itba.harmos.dtos.requests

data class ForgotPasswordRequest(
    val email: String
) {
    fun isValid(): Boolean {
        return email.isNotBlank() && isValidEmail(email)
    }

    fun getValidationError(): String? {
        return when {
            email.isBlank() -> "El email es obligatorio"
            !isValidEmail(email) -> "El email debe tener un formato válido"
            else -> null
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }
} 