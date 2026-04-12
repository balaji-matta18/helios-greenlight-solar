package com.productapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SurveyorSubmitRequest {

    @NotBlank(message = "Service number is required")
    private String serviceNumber;

    @NotBlank(message = "Surveyor name is required")
    private String surveyorName;

    private String inverterSerialNumber;
    private String panelNumber1;
    private String panelNumber2;
    private String panelNumber3;
    private String panelNumber4;
}