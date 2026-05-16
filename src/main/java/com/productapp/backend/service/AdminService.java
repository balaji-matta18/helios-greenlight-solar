// ─────────────────────────────────────────────────────────────────────────────
// AdminService.java
// FIX (SECURITY): admin login no longer reveals whether an email exists.
// Previously AdminNotFoundException was thrown for unknown emails, which let
// an attacker enumerate valid admin emails by watching the error responses.
// Now both "not found" and "wrong password" return the same generic error.
// ─────────────────────────────────────────────────────────────────────────────
package com.productapp.backend.service;

import com.productapp.backend.dto.AdminLoginRequest;
import com.productapp.backend.dto.ApiResponse;
import com.productapp.backend.dto.AuthResponse;
import com.productapp.backend.entity.Admin;
import com.productapp.backend.entity.OtpType;
import com.productapp.backend.exception.InvalidCredentialsException;
import com.productapp.backend.repository.AdminRepository;
import com.productapp.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    public ApiResponse login(AdminLoginRequest request) {

        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElse(null);

        // FIX: unified error response — don't reveal whether the email exists
        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            log.warn("Admin login failed for: {}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        otpService.sendOtp(admin.getEmail(), OtpType.ADMIN_2FA);
        log.info("Admin password verified, 2FA OTP sent: {}", admin.getEmail());
        return new ApiResponse("Password verified. OTP sent to your registered email.");
    }

    public AuthResponse verify2fa(String email, String otpValue) {
        otpService.verifyOtpOnly(email, otpValue, OtpType.ADMIN_2FA);
        String token = jwtService.generateToken(email, "ROLE_ADMIN");
        log.info("Admin 2FA verified, JWT issued for: {}", email);
        return AuthResponse.builder()
                .token(token)
                .role("ROLE_ADMIN")
                .mobileNumber(email)
                .build();
    }
}
