package ar.edu.itba.harmos.models

data class EmailTemplate(
    val subject: String,
    val body: String,
    val isHtml: Boolean = true
) {
    companion object {
        fun passwordReset(resetLink: String): EmailTemplate {
            return EmailTemplate(
                subject = "Restablecer contraseña - Harmos",
                body = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                        <div style="text-align: center; margin-bottom: 30px;">
                            <h1 style="color: #10A6AC; margin: 0;">Harmos</h1>
                        </div>
                        <div style="background-color: #f9f9f9; padding: 20px; border-radius: 5px;">
                            <h2 style="color: #333; margin-top: 0;">Restablecer contraseña</h2>
                            <p style="color: #666; line-height: 1.6;">
                                Hemos recibido una solicitud para restablecer tu contraseña. 
                                Si no realizaste esta solicitud, puedes ignorar este correo.
                            </p>
                            <p style="color: #666; line-height: 1.6;">
                                Para restablecer tu contraseña, haz clic en el siguiente botón:
                            </p>
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="$resetLink" 
                                   style="background-color: #10A6AC; color: white; padding: 12px 24px; 
                                          text-decoration: none; border-radius: 5px; display: inline-block;">
                                    Restablecer contraseña
                                </a>
                            </div>
                            <p style="color: #666; line-height: 1.6;">
                                O copia y pega el siguiente enlace en tu navegador:
                            </p>
                            <p style="color: #10A6AC; word-break: break-all;">
                                <a href="$resetLink" style="color: #10A6AC;">$resetLink</a>
                            </p>
                            <p style="color: #666; line-height: 1.6;">
                                Este enlace expirará en 24 horas por razones de seguridad.
                            </p>
                        </div>
                        <div style="text-align: center; margin-top: 30px; color: #999; font-size: 12px;">
                            <p>Este es un correo automático, por favor no respondas a este mensaje.</p>
                        </div>
                    </div>
                """.trimIndent()
            )
        }
    }
} 