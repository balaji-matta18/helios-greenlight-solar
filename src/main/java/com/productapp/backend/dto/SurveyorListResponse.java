package com.productapp.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SurveyorListResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private long submissionCount;
    private boolean active;
    private LocalDateTime createdAt;
}