package com.productapp.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AllowedEmailResponse {
    private Long id;
    private String email;
    private LocalDateTime createdAt;
}