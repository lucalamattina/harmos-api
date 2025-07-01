package ar.edu.itba.harmos.dtos.responses

data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String,
    val error: String? = null,
    val details: Map<String, Any>? = null
) {
    companion object {
        fun success(message: String = "Se ha enviado un email con instrucciones para recuperar tu contraseña"): ForgotPasswordResponse {
            return ForgotPasswordResponse(
                success = true,
                message = message
            )
        }

        fun error(error: String, details: Map<String, Any>? = null): ForgotPasswordResponse {
            return ForgotPasswordResponse(
                success = false,
                message = "",
                error = error,
                details = details
            )
        }

        fun validationError(error: String, receivedEmail: String): ForgotPasswordResponse {
            return ForgotPasswordResponse(
                success = false,
                message = "",
                error = error,
                details = mapOf(
                    "validation" to "email_format",
                    "received_email" to receivedEmail,
                    "email_length" to receivedEmail.length,
                    "suggestion" to "Asegúrate de que el email tenga el formato: usuario@dominio.com"
                )
            )
        }
    }
} 