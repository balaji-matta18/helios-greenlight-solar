//package com.productapp.backend.service;
//
//import com.productapp.backend.dto.AuthResponse;
//import com.productapp.backend.entity.Otp;
//import com.productapp.backend.entity.OtpType;
//import com.productapp.backend.exception.InvalidOtpException;
//import com.productapp.backend.exception.OtpExpiredException;
//import com.productapp.backend.exception.OtpNotFoundException;
//import com.productapp.backend.exception.TooManyOtpRequestsException;
//import com.productapp.backend.repository.OtpRepository;
//import com.productapp.backend.security.JwtService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.security.SecureRandom;
//import java.time.LocalDateTime;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class OtpService {
//
//    private final OtpRepository otpRepository;
//    private final JwtService jwtService;
//    private final EmailService emailService;
//
//    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
//    private static final int OTP_COOLDOWN_SECONDS = 60;
//    private static final int OTP_EXPIRY_MINUTES = 5;
//
//    public void sendOtp(String email, OtpType otpType) {
//
//        LocalDateTime cooldownSince = LocalDateTime.now().minusSeconds(OTP_COOLDOWN_SECONDS);
//        if (otpRepository.existsRecentOtp(email, otpType, cooldownSince)) {
//            log.warn("OTP rate limit hit for email: {} type: {}", email, otpType);
//            throw new TooManyOtpRequestsException(OTP_COOLDOWN_SECONDS);
//        }
//
//        String otpValue = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
//
////        // Remove this line while we got SES production access
////        log.info("DEV OTP for {} : {}", email, otpValue);
//
//        Otp otp = Otp.builder()
//                .email(email)
//                .otp(otpValue)
//                .otpType(otpType)
//                .build();
//
//        otpRepository.save(otp);
//        emailService.sendOtpEmail(email, otpValue, OTP_EXPIRY_MINUTES);
//
//        log.info("OTP sent to email: {} type: {}", email, otpType);
//    }
//
//    public void verifyOtpOnly(String email, String otpValue, OtpType otpType) {
//
//        Otp otp = otpRepository
//                .findTopByEmailAndOtpTypeOrderByCreatedAtDesc(email, otpType)
//                .orElseThrow(() -> new OtpNotFoundException(email));
//
//        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
//            throw new OtpExpiredException();
//        }
//
//        if (!otp.getOtp().equals(otpValue)) {
//            throw new InvalidOtpException();
//        }
//
//        otp.setVerified(true);
//        otpRepository.save(otp);
//
//        log.info("OTP verified for email: {} type: {}", email, otpType);
//    }
//
//    // Used by surveyor login — verifies OTP then returns JWT
//    public AuthResponse verifyOtpAndLogin(String email, String otpValue, OtpType otpType) {
//
//        verifyOtpOnly(email, otpValue, otpType);
//
//        String token = jwtService.generateToken(email, "ROLE_SURVEYOR");
//
//        return AuthResponse.builder()
//                .token(token)
//                .role("ROLE_SURVEYOR")
//                .mobileNumber(email)
//                .build();
//    }
//}







package com.productapp.backend.service;

import com.productapp.backend.dto.AuthResponse;
import com.productapp.backend.entity.Otp;
import com.productapp.backend.entity.OtpType;
import com.productapp.backend.exception.InvalidOtpException;
import com.productapp.backend.exception.OtpExpiredException;
import com.productapp.backend.exception.OtpNotFoundException;
import com.productapp.backend.exception.TooManyOtpRequestsException;
import com.productapp.backend.repository.OtpRepository;
import com.productapp.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final JwtService jwtService;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_COOLDOWN_SECONDS = 60;
    private static final int OTP_EXPIRY_MINUTES = 5;

    public void sendOtp(String email, OtpType otpType) {

        LocalDateTime cooldownSince = LocalDateTime.now().minusSeconds(OTP_COOLDOWN_SECONDS);
        if (otpRepository.existsRecentOtp(email, otpType, cooldownSince)) {
            log.warn("OTP rate limit hit for email: {} type: {}", email, otpType);
            throw new TooManyOtpRequestsException(OTP_COOLDOWN_SECONDS);
        }

        String otpValue = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));

        // Remove this line while we got SES production access
        log.info("DEV OTP for {} : {}", email, otpValue);

        Otp otp = Otp.builder()
                .email(email)
                .otp(otpValue)
                .otpType(otpType)
                .build();

        otpRepository.save(otp);
        try {
            emailService.sendOtpEmail(email, otpValue, OTP_EXPIRY_MINUTES);
        } catch (Exception e) {
            log.warn("SES not available (dev mode) – OTP logged above. Error: {}", e.getMessage());
        }

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

    // Used by surveyor login — verifies OTP then returns JWT
    public AuthResponse verifyOtpAndLogin(String email, String otpValue, OtpType otpType) {

        verifyOtpOnly(email, otpValue, otpType);

        String token = jwtService.generateToken(email, "ROLE_SURVEYOR");

        return AuthResponse.builder()
                .token(token)
                .role("ROLE_SURVEYOR")
                .mobileNumber(email)
                .build();
    }
}