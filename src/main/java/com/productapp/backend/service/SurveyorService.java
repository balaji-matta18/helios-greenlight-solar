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

    // Signup — password is hashed before saving
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

    // Login step 1 — verify email + password, send OTP
    public ApiResponse login(SurveyorLoginRequest request) {

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
}