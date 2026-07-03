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

        fun announcementNotification(
            announcementTitle: String,
            announcementContent: String,
            link: String,
            author: String? = null,
            date: String? = null,
            specialties: String? = null,
            imageUrl: String? = null
        ): EmailTemplate {
            val year = java.time.Year.now()
            val specialtiesHtml = if (!specialties.isNullOrBlank()) {
                """<div style=\"font-size: 15px; color: #666; margin-bottom: 12px;\"><strong>Especialidades:</strong> $specialties</div>"""
            } else ""
            val authorDateHtml = buildString {
                if (!date.isNullOrBlank()) append("Publicado el $date")
                if (!author.isNullOrBlank()) append(" por $author")
            }
            val imageHtml = if (!imageUrl.isNullOrBlank()) {
                """<img src=\"$imageUrl\" alt=\"Imagen del anuncio\" style=\"width:100%;max-height:220px;object-fit:cover;display:block; margin-bottom: 18px; border-radius: 8px;\" />"""
            } else ""

            return EmailTemplate(
                subject = "Nuevo anuncio: $announcementTitle",
                body = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                        <div style="text-align: center; margin-bottom: 30px;">
                            <h1 style="color: #10A6AC; margin: 0;">Harmos</h1>
                        </div>
                        <div style="background-color: #f9f9f9; padding: 20px; border-radius: 5px;">
                            <h2 style="color: #333; margin-top: 0;">$announcementTitle</h2>
                            $imageHtml
                            $specialtiesHtml
                            <div style="color: #666; margin-bottom: 15px; font-size: 14px;">
                                $authorDateHtml
                            </div>
                            <div style="color: #666; line-height: 1.6; margin-bottom: 20px;">
                                $announcementContent
                            </div>
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="$link" 
                                   style="background-color: #10A6AC; color: white; padding: 12px 24px; 
                                          text-decoration: none; border-radius: 5px; display: inline-block;">
                                    Ver anuncio completo
                                </a>
                            </div>
                        </div>
                        <div style="text-align: center; margin-top: 30px; color: #999; font-size: 12px;">
                            <p>Este es un correo automático, por favor no respondas a este mensaje.</p>
                            <p>© $year Harmos. Todos los derechos reservados.</p>
                        </div>
                    </div>
                """.trimIndent()
            )
        }

        fun reportCreated(
            reportTitle: String,
            patientName: String,
            creatorName: String,
            link: String
        ): EmailTemplate {
            val subject = "Nuevo Reporte: $reportTitle"
            val body = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: sans-serif; }
                    .container { padding: 20px; }
                    .header { font-size: 24px; color: #333; }
                    .content { margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">Nuevo Reporte</div>
                    <div class="content">
                        <p>Hola,</p>
                        <p>Se ha creado un nuevo reporte "<strong>$reportTitle</strong>" para el paciente <strong>$patientName</strong> por <strong>$creatorName</strong>.</p>
                        <p>Puedes ver los detalles en el siguiente enlace:</p>
                        <p><a href="$link">Ver Reporte</a></p>
                    </div>
                </div>
            </body>
            </html>
            """.trimIndent()
            return EmailTemplate(subject, body, true)
        }

        fun reportModified(
            reportTitle: String,
            patientName: String,
            editorName: String,
            link: String
        ): EmailTemplate {
            val subject = "Reporte Modificado: $reportTitle"
            val body = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: sans-serif; }
                    .container { padding: 20px; }
                    .header { font-size: 24px; color: #333; }
                    .content { margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">Reporte Modificado</div>
                    <div class="content">
                        <p>Hola,</p>
                        <p>El reporte "<strong>$reportTitle</strong>" para el paciente <strong>$patientName</strong> ha sido modificado por <strong>$editorName</strong>.</p>
                        <p>Puedes ver los detalles en el siguiente enlace:</p>
                        <p><a href="$link">Ver Reporte</a></p>
                    </div>
                </div>
            </body>
            </html>
            """.trimIndent()
            return EmailTemplate(subject, body, true)
        }
    }
} 