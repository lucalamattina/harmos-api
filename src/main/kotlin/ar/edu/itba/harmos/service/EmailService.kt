package ar.edu.itba.harmos.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService @Autowired constructor(
    private val mailSender: JavaMailSender
) {
    fun sendEmail(to: String, subject: String, body: String) {
        val message = SimpleMailMessage()
        message.setTo(to)
        message.setSubject(subject)
        message.setText(body)
        mailSender.send(message)
    }
} 