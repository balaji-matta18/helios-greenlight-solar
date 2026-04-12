package com.productapp.backend.service;

import com.productapp.backend.dto.AdminLoginRequest;
import com.productapp.backend.dto.ApiResponse;
import com.productapp.backend.dto.AuthResponse;
import com.productapp.backend.entity.Admin;
import com.productapp.backend.entity.OtpType;
import com.productapp.backend.exception.AdminNotFoundException;
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

    // Step 1 — validate password, send 2FA OTP
    public ApiResponse login(AdminLoginRequest request) {

        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed — admin not found: {}", request.getUsername());
                    return new AdminNotFoundException(request.getUsername());
                });

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            log.warn("Login failed — wrong password for admin: {}", request.getUsername());
            throw new InvalidCredentialsException();
        }

        otpService.sendOtp(admin.getEmail(), OtpType.ADMIN_2FA);
        log.info("Admin password verified, 2FA OTP sent: {}", admin.getUsername());

        return new ApiResponse("Password verified. OTP sent to your registered email.");
    }

    // Step 2 — verify 2FA OTP, return JWT
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