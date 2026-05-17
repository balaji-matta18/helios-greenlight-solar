package com.productapp.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long id;
    private String editedByEmail;
    private String editedByRole;
    private String editNote;
    private LocalDateTime editedAt;
}