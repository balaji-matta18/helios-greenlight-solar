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

    @PostMapping("/surveyor/login")
    public ResponseEntity<ApiResponse> surveyorLogin(
            @Valid @RequestBody SurveyorLoginRequest request) {
        return ResponseEntity.ok(surveyorService.login(request));
    }

    @PostMapping("/surveyor/verify-otp")
    public ResponseEntity<AuthResponse> surveyorVerifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(otpService.verifyOtpAndLogin(
                request.getEmail(), request.getOtp(), OtpType.SURVEYOR_LOGIN));
    }

    @PostMapping("/surveyor/signup")
    public ResponseEntity<ApiResponse> surveyorSignup(
            @Valid @RequestBody SurveyorSignupRequest request) {
        surveyorService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Surveyor registered successfully"));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse> adminLogin(
            @Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminService.login(request));
    }

    @PostMapping("/admin/verify-2fa")
    public ResponseEntity<AuthResponse> adminVerify2fa(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(adminService.verify2fa(request.getEmail(), request.getOtp()));
    }

    @PostMapping("/surveyor/forgot-password")
    public ResponseEntity<ApiResponse> surveyorForgotPassword(
            @Valid @RequestBody SendOtpRequest request) {
        return ResponseEntity.ok(surveyorService.sendForgotPasswordOtp(request.getEmail()));
    }

    @PostMapping("/surveyor/reset-password")
    public ResponseEntity<ApiResponse> surveyorResetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(surveyorService.resetPassword(
                request.getEmail(), request.getOtp(), request.getNewPassword()));
    }
}