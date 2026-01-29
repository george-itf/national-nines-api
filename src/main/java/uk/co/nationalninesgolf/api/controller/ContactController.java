package uk.co.nationalninesgolf.api.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Contact form submission endpoint
 */
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"https://nationalninesgolf.co.uk", "http://localhost:4321", "http://localhost:3000"})
public class ContactController {
    
    private final JavaMailSender mailSender;
    
    @Value("${app.admin-email:info@nationalninesgolf.co.uk}")
    private String adminEmail;
    
    @Value("${spring.mail.username:noreply@nationalninesgolf.co.uk}")
    private String fromEmail;
    
    @Data
    public static class ContactForm {
        @NotBlank
        private String name;
        
        @NotBlank
        @Email
        private String email;
        
        @NotBlank
        private String subject;
        
        @NotBlank
        private String message;
        
        private String phone;
    }
    
    @PostMapping
    public ResponseEntity<Map<String, String>> submitContact(@Valid @RequestBody ContactForm form) {
        try {
            // Send to admin
            String adminBody = String.format("""
                New contact form submission
                
                From: %s (%s)
                Phone: %s
                Subject: %s
                
                Message:
                %s
                
                Received: %s
                """,
                form.getName(),
                form.getEmail(),
                form.getPhone() != null ? form.getPhone() : "-",
                form.getSubject(),
                form.getMessage(),
                LocalDateTime.now()
            );
            
            SimpleMailMessage adminMessage = new SimpleMailMessage();
            adminMessage.setFrom(fromEmail);
            adminMessage.setTo(adminEmail);
            adminMessage.setReplyTo(form.getEmail());
            adminMessage.setSubject("Contact Form: " + form.getSubject());
            adminMessage.setText(adminBody);
            mailSender.send(adminMessage);
            
            // Send confirmation to user
            String userBody = String.format("""
                Dear %s,
                
                Thank you for contacting National Nines Golf!
                
                We've received your message and will get back to you as soon as possible.
                
                Your message:
                ---
                Subject: %s
                
                %s
                ---
                
                Best regards,
                The National Nines Team
                https://nationalninesgolf.co.uk
                """,
                form.getName(),
                form.getSubject(),
                form.getMessage()
            );
            
            SimpleMailMessage userMessage = new SimpleMailMessage();
            userMessage.setFrom(fromEmail);
            userMessage.setTo(form.getEmail());
            userMessage.setSubject("We've received your message - National Nines Golf");
            userMessage.setText(userBody);
            mailSender.send(userMessage);
            
            log.info("Contact form submitted by {} ({})", form.getName(), form.getEmail());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Thank you for your message. We'll be in touch soon!"
            ));
            
        } catch (Exception e) {
            log.error("Failed to process contact form", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to send message. Please try again or email us directly."
            ));
        }
    }
}
