package com.productapp.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SubmissionResponse {

    private Long id;

    // Admin-filled
    private String serviceNumber;
    private String customerName;
    private String phone;
    private String address;
    private String division;
    private String subDivision;
    private String section;
    private String distribution;

    // Surveyor-filled
    private String inverterSerialNumber;
    private String surveyorName;
    private String surveyorEmail;
    private List<PanelNumberDto> panelNumbers;
    private List<ImageDto> images;

    // Status
    private String status;
    private String rejectionReason;  // populated only when status = REJECTED

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lastUpdatedBy;    // always "Name (Role)" format

    @Data
    @Builder
    public static class PanelNumberDto {
        private Integer sequence;
        private String panelNumber;
    }

    @Data
    @Builder
    public static class ImageDto {
        private Long id;
        private String imageType;
        private String imageUrl;
    }
}