package com.aisummarizer.website.services;


import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String verificationLink) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Verify your AlphaBeta account");

            // Load HTML template
            String html = new String(
                    getClass().getResourceAsStream(
                            "/templates/verification-email.html"
                    ).readAllBytes(),
                    StandardCharsets.UTF_8
            );

            html = html.replace("{{verificationLink}}", verificationLink);

            helper.setText(html, true);

            // Embed logo
            helper.addInline(
                    "alphabeta-logo",
                    new ClassPathResource("static/email/logo.png")
            );

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
