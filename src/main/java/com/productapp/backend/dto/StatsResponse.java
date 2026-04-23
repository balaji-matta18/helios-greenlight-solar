package com.productapp.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsResponse {
    private long total;
    private long pending;
    private long submitted;
    private long surveyorCount;
}