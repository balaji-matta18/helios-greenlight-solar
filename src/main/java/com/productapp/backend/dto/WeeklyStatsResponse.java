package com.productapp.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class WeeklyStatsResponse {

    private List<DayCount> days;

    @Data
    @Builder
    public static class DayCount {
        private LocalDate date;
        private String dayLabel;
        private long count;
    }
}