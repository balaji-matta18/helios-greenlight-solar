package com.productapp.backend.service;

import com.productapp.backend.dto.StatsResponse;
import com.productapp.backend.dto.SurveyorListResponse;
import com.productapp.backend.dto.WeeklyStatsResponse;
import com.productapp.backend.entity.Surveyor;
import com.productapp.backend.entity.SubmissionStatus;
import com.productapp.backend.repository.SubmissionRepository;
import com.productapp.backend.repository.SurveyorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final SubmissionRepository submissionRepository;
    private final SurveyorRepository   surveyorRepository;

    public StatsResponse getDashboardStats() {
        long pendingCount = submissionRepository.countByStatus(SubmissionStatus.PENDING);
        long unassignedCount = submissionRepository.countByStatusAndSurveyorIsNull(SubmissionStatus.PENDING);
        return StatsResponse.builder()
                .total(submissionRepository.count())
                .pending(pendingCount)
                .submitted(submissionRepository.countByStatus(SubmissionStatus.SUBMITTED))
                .approved(submissionRepository.countByStatus(SubmissionStatus.APPROVED))
                .rejected(submissionRepository.countByStatus(SubmissionStatus.REJECTED))
                .unassigned(unassignedCount)
                .surveyorCount(surveyorRepository.count())
                .build();
    }

    public WeeklyStatsResponse getWeeklyStats() {

        LocalDateTime since = LocalDate.now().minusDays(6).atStartOfDay();
        List<Object[]> rows = submissionRepository.countByDaySince(since);

        Map<LocalDate, Long> countByDate = rows.stream()
                .collect(Collectors.toMap(r -> (LocalDate) r[0], r -> (Long) r[1]));

        List<WeeklyStatsResponse.DayCount> days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            days.add(WeeklyStatsResponse.DayCount.builder()
                    .date(date)
                    .dayLabel(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .count(countByDate.getOrDefault(date, 0L))
                    .build());
        }
        return WeeklyStatsResponse.builder().days(days).build();
    }

    public List<SurveyorListResponse> getSurveyorList() {

        List<Surveyor> surveyors = surveyorRepository.findAll();

        Map<Long, Long> countMap = submissionRepository.countBySurveyor()
                .stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        return surveyors.stream().map(s -> {
            long count = countMap.getOrDefault(s.getId(), 0L);
            return SurveyorListResponse.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .email(s.getEmail())
                    .phone(s.getPhone())
                    .submissionCount(count)
                    .active(count > 0)
                    .createdAt(s.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }
}