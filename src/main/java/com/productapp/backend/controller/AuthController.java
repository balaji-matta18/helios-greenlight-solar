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

    // ── Surveyor auth ─────────────────────────────────────────────────────────

    @PostMapping("/surveyor/send-otp")
    public ResponseEntity<ApiResponse> surveyorSendOtp(@Valid @RequestBody SendOtpRequest request) {
        otpService.sendOtp(request.getEmail(), OtpType.SURVEYOR_LOGIN);
        return ResponseEntity.ok(new ApiResponse("OTP sent to " + request.getEmail()));
    }

    @PostMapping("/surveyor/verify-otp")
    public ResponseEntity<AuthResponse> surveyorVerifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = otpService.verifyOtpAndLogin(
                request.getEmail(), request.getOtp(), OtpType.SURVEYOR_LOGIN);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/surveyor/signup")
    public ResponseEntity<ApiResponse> surveyorSignup(@Valid @RequestBody SurveyorSignupRequest request) {
        surveyorService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Surveyor registered successfully"));
    }

    // ── Admin auth ────────────────────────────────────────────────────────────

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        ApiResponse response = adminService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/verify-2fa")
    public ResponseEntity<AuthResponse> adminVerify2fa(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = adminService.verify2fa(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(response);
    }
}