package com.productapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminSubmissionRequest {

    @NotBlank(message = "Service number is required")
    private String serviceNumber;

    private String customerName;
    private String phone;
    private String address;
    private String division;
    private String subDivision;
    private String section;
    private String distribution;
    private String inverterSerialNumber;
    private String surveyorName;

    // panel numbers 1-4
    private String panelNumber1;
    private String panelNumber2;
    private String panelNumber3;
    private String panelNumber4;
}