package com.productapp.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignSurveyorRequest {

    @NotNull(message = "Surveyor ID is required")
    private Long surveyorId;
}
