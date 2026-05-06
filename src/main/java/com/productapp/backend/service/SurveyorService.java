package com.productapp.backend.service;

import com.productapp.backend.dto.ApiResponse;
import com.productapp.backend.dto.SurveyorLoginRequest;
import com.productapp.backend.dto.SurveyorSignupRequest;
import com.productapp.backend.entity.OtpType;
import com.productapp.backend.entity.Surveyor;
import com.productapp.backend.exception.*;
import com.productapp.backend.repository.AllowedEmailRepository;
import com.productapp.backend.repository.SurveyorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyorService {

    private final SurveyorRepository surveyorRepository;
    private final AllowedEmailRepository allowedEmailRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;


    public void signup(SurveyorSignupRequest request) {

        if (!allowedEmailRepository.existsByEmail(request.getEmail())) {
            log.warn("Signup rejected — not in whitelist: {}", request.getEmail());
            throw new EmailNotAllowedException(request.getEmail());
        }
        if (surveyorRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException(request.getEmail());
        }

        Surveyor surveyor = Surveyor.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        surveyorRepository.save(surveyor);
        log.info("Surveyor registered: {}", request.getEmail());
    }


    public ApiResponse login(SurveyorLoginRequest request) {

        // Access check first — if admin removed this email from the whitelist,
        // the surveyor account is suspended. All their data is preserved; they
        // simply cannot authenticate until the email is re-added by an admin.
        if (!allowedEmailRepository.existsByEmail(request.getEmail())) {
            log.warn("Surveyor login blocked — email removed from whitelist: {}", request.getEmail());
            throw new EmailNotAllowedException(request.getEmail());
        }

        Surveyor surveyor = surveyorRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Surveyor login failed — not found: {}", request.getEmail());
                    return new SurveyorNotFoundException(request.getEmail());
                });

        if (!passwordEncoder.matches(request.getPassword(), surveyor.getPassword())) {
            log.warn("Surveyor login failed — wrong password: {}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        otpService.sendOtp(surveyor.getEmail(), OtpType.SURVEYOR_LOGIN);
        log.info("Surveyor password verified, OTP sent: {}", surveyor.getEmail());
        return new ApiResponse("Password verified. OTP sent to your registered email.");
    }

    public Surveyor getByEmail(String email) {
        return surveyorRepository.findByEmail(email)
                .orElseThrow(() -> new SurveyorNotFoundException(email));
    }

    public ApiResponse sendForgotPasswordOtp(String email) {
        surveyorRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Forgot-password attempt for unknown email: {}", email);
                    return new SurveyorNotFoundException(email);
                });

        otpService.sendOtp(email, OtpType.PASSWORD_RESET);
        log.info("Password-reset OTP sent for: {}", email);
        return new ApiResponse("OTP sent to your registered email.");
    }

    public ApiResponse resetPassword(String email, String otpValue, String newPassword) {
        otpService.verifyOtpOnly(email, otpValue, OtpType.PASSWORD_RESET);

        Surveyor surveyor = surveyorRepository.findByEmail(email)
                .orElseThrow(() -> new SurveyorNotFoundException(email));

        surveyor.setPassword(passwordEncoder.encode(newPassword));
        surveyorRepository.save(surveyor);

        log.info("Password reset successfully for: {}", email);
        return new ApiResponse("Password reset successfully.");
    }
}