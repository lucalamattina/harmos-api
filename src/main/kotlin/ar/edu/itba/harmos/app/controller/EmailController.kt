package ar.edu.itba.harmos.app.controller

import ar.edu.itba.harmos.dtos.requests.EmailRequest
import ar.edu.itba.harmos.dtos.responses.EmailResponse
import ar.edu.itba.harmos.services.EmailService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/messages")
class EmailController @Autowired constructor(
    private val emailService: EmailService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createMessage(@RequestBody request: EmailRequest): ResponseEntity<EmailResponse> {
        emailService.sendEmail(request.to, request.subject, request.body)
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(EmailResponse(
                recipient = request.to,
                subject = request.subject
            ))
    }

    @GetMapping("/{id}")
    fun getMessageStatus(@PathVariable id: String): ResponseEntity<EmailResponse> {
        // In a real implementation, you would store and retrieve message status
        // For now, we'll return a mock response
        return ResponseEntity.ok(
            EmailResponse(
                id = id,
                recipient = "example@email.com",
                subject = "Example Subject"
            )
        )
    }
} 