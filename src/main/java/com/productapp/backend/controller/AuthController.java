package com.productapp.backend.controller;

import com.productapp.backend.dto.*;
import com.productapp.backend.entity.OtpType;
import com.productapp.backend.service.AdminService;
import com.productapp.backend.service.OtpService;
import com.productapp.backend.service.SurveyorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OtpService otpService;
    private final AdminService adminService;
    private final SurveyorService surveyorService;

    // ── Surveyor: step 1 — email + password → sends OTP ──────────────────────
    @PostMapping("/surveyor/login")
    public ResponseEntity<ApiResponse> surveyorLogin(
            @Valid @RequestBody SurveyorLoginRequest request) {
        return ResponseEntity.ok(surveyorService.login(request));
    }

    // ── Surveyor: step 2 — OTP → JWT ─────────────────────────────────────────
    @PostMapping("/surveyor/verify-otp")
    public ResponseEntity<AuthResponse> surveyorVerifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(otpService.verifyOtpAndLogin(
                request.getEmail(), request.getOtp(), OtpType.SURVEYOR_LOGIN));
    }

    // ── Surveyor: signup ──────────────────────────────────────────────────────
    @PostMapping("/surveyor/signup")
    public ResponseEntity<ApiResponse> surveyorSignup(
            @Valid @RequestBody SurveyorSignupRequest request) {
        surveyorService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Surveyor registered successfully"));
    }

    // ── Admin: step 1 — email + password → sends OTP ─────────────────────────
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse> adminLogin(
            @Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminService.login(request));
    }

    // ── Admin: step 2 — OTP → JWT ─────────────────────────────────────────────
    @PostMapping("/admin/verify-2fa")
    public ResponseEntity<AuthResponse> adminVerify2fa(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(adminService.verify2fa(request.getEmail(), request.getOtp()));
    }
}