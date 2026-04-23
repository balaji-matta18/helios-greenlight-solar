package com.productapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminEditRequest extends AdminSubmissionRequest {

    @NotBlank(message = "Edit note is required — describe what you changed")
    @Size(max = 500, message = "Edit note must not exceed 500 characters")
    private String editNote;
}