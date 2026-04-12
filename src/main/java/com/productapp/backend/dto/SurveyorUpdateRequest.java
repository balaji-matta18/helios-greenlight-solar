package com.productapp.backend.dto;

import lombok.Data;

@Data
public class SurveyorUpdateRequest {
    // surveyor name intentionally excluded — not editable after submission
    private String inverterSerialNumber;
    private String panelNumber1;
    private String panelNumber2;
    private String panelNumber3;
    private String panelNumber4;
}