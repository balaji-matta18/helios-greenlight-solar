package com.productapp.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.sender}")
    private String senderEmail;

    public void sendOtpEmail(String toEmail, String otpValue, int expiryMinutes) {
        String subject = "Your Helios OTP Code";
        String htmlBody = """
                <html>
                  <body style="font-family: Arial, sans-serif; padding: 24px; max-width: 480px; margin: auto;">
                    <h2 style="color: #4e9d8a;">Helios Green Light Solar</h2>
                    <p style="color: #333;">Your one-time password (OTP) is:</p>
                    <div style="font-size: 36px; font-weight: 700; letter-spacing: 10px;
                                color: #1a1a1a; margin: 20px 0;">%s</div>
                    <p style="color: #555;">
                      This code is valid for <strong>%d minutes</strong>.
                      Do not share it with anyone.
                    </p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;" />
                    <p style="color: #aaa; font-size: 12px;">
                      If you did not request this, please ignore this email.
                    </p>
                  </body>
                </html>
                """.formatted(otpValue, expiryMinutes);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }
}