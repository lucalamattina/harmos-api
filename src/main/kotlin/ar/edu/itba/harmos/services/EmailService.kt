package ar.edu.itba.harmos.services

import ar.edu.itba.harmos.models.EmailTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import javax.mail.internet.MimeMessage

@Service
class EmailService @Autowired constructor(
    private val mailSender: JavaMailSender
) {
    fun sendEmail(to: String, template: EmailTemplate) {
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        
        helper.setTo(to)
        helper.setSubject(template.subject)
        helper.setText(template.body, template.isHtml)
        
        mailSender.send(message)
    }

    fun sendPasswordResetEmail(to: String, resetLink: String) {
        val template = EmailTemplate.passwordReset(resetLink)
        sendEmail(to, template)
    }
} 