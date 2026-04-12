package com.productapp.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final SesClient sesClient;

    @Value("${aws.ses.sender-email}")
    private String senderEmail;

    public void sendOtpEmail(String toEmail, String otpValue, int expiryMinutes) {
        String subject = "Your SolarFreelance OTP Code";
        String htmlBody = """
                <html>
                  <body style="font-family: Arial, sans-serif; padding: 24px;">
                    <h2 style="color: #1976D2;">SolarFreelance</h2>
                    <p>Your one-time password (OTP) is:</p>
                    <h1 style="letter-spacing: 8px; color: #333;">%s</h1>
                    <p>This code is valid for <b>%d minutes</b>. Do not share it with anyone.</p>
                    <p style="color: #999; font-size: 12px;">If you did not request this, please ignore this email.</p>
                  </body>
                </html>
                """.formatted(otpValue, expiryMinutes);

        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(senderEmail)
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(request);
            log.info("OTP email sent to: {}", toEmail);

        } catch (SesException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }
}