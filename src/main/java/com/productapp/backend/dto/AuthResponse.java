package com.productapp.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String role;
    private String mobileNumber;
    private String email;
    private String name;
}