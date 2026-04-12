package com.productapp.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubmissionSummaryResponse {
    private Long id;
    private String serviceNumber;
    private String customerName;
    private String division;
    private String section;
    private String surveyorName;
    private String status;
    private LocalDateTime createdAt;
}