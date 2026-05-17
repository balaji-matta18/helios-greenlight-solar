package com.productapp.backend.service;

import com.productapp.backend.dto.AuthResponse;
import com.productapp.backend.entity.Otp;
import com.productapp.backend.entity.OtpType;
import com.productapp.backend.entity.Surveyor;
import com.productapp.backend.exception.InvalidOtpException;
import com.productapp.backend.exception.OtpExpiredException;
import com.productapp.backend.exception.OtpNotFoundException;
import com.productapp.backend.exception.TooManyOtpRequestsException;
import com.productapp.backend.repository.OtpRepository;
import com.productapp.backend.repository.SurveyorRepository;
import com.productapp.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository      otpRepository;
    private final JwtService         jwtService;
    private final EmailService       emailService;
    private final SurveyorRepository surveyorRepository;

    private static final SecureRandom SECURE_RANDOM       = new SecureRandom();
    private static final int          OTP_COOLDOWN_SECONDS = 60;
    private static final int          OTP_EXPIRY_MINUTES   = 5;

    public void sendOtp(String email, OtpType otpType) {

        LocalDateTime cooldownSince = LocalDateTime.now().minusSeconds(OTP_COOLDOWN_SECONDS);
        if (otpRepository.existsRecentOtp(email, otpType, cooldownSince)) {
            log.warn("OTP rate limit hit for email: {} type: {}", email, otpType);
            throw new TooManyOtpRequestsException(OTP_COOLDOWN_SECONDS);
        }

        String otpValue = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));

        otpRepository.save(Otp.builder()
                .email(email)
                .otp(otpValue)
                .otpType(otpType)
                .build());

        // @Async — returns immediately, email sent in background thread
        emailService.sendOtpEmail(email, otpValue, OTP_EXPIRY_MINUTES);

        log.info("OTP sent to email: {} type: {}", email, otpType);
    }

    public void verifyOtpOnly(String email, String otpValue, OtpType otpType) {

        Otp otp = otpRepository
                .findTopByEmailAndOtpTypeOrderByCreatedAtDesc(email, otpType)
                .orElseThrow(() -> new OtpNotFoundException(email));

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new OtpExpiredException();
        }

        if (!otp.getOtp().equals(otpValue)) {
            throw new InvalidOtpException();
        }

        otp.setVerified(true);
        otpRepository.save(otp);

        log.info("OTP verified for email: {} type: {}", email, otpType);
    }

    public AuthResponse verifyOtpAndLogin(String email, String otpValue, OtpType otpType) {

        verifyOtpOnly(email, otpValue, otpType);

        Surveyor surveyor = surveyorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Surveyor not found: " + email));

        String token = jwtService.generateToken(email, "ROLE_SURVEYOR", surveyor.getName());

        log.info("Surveyor OTP verified, JWT issued for: {}", email);
        return AuthResponse.builder()
                .token(token)
                .role("ROLE_SURVEYOR")
                .email(email)
                .name(surveyor.getName())
                .build();
    }

    // FIX: scheduled cleanup — runs every hour and deletes OTPs that expired more
    // than 1 hour ago. Without this the otp table grows forever and slows down
    // the existsRecentOtp and findTop queries over time.
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanExpiredOtps() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        otpRepository.deleteExpiredOtps(cutoff);
        log.info("Expired OTPs cleaned up");
    }
}