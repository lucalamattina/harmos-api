package ar.edu.itba.harmos.dtos.responses

data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String,
    val error: String? = null
) {
    companion object {
        fun success(message: String = "Se ha enviado un email con instrucciones para recuperar tu contraseña"): ForgotPasswordResponse {
            return ForgotPasswordResponse(
                success = true,
                message = message
            )
        }

        fun error(error: String): ForgotPasswordResponse {
            return ForgotPasswordResponse(
                success = false,
                message = "",
                error = error
            )
        }
    }
} 