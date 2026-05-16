package com.productapp.backend.repository;

import com.productapp.backend.entity.Submission;
import com.productapp.backend.entity.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface SubmissionRepositoryCustom {

    // Admin — paginated with all filters
    Page<Submission> findAllFilteredPaged(
            Long surveyorId,
            SubmissionStatus status,
            String serviceNumber,
            String division,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    // Surveyor — own submissions with optional filters
    Page<Submission> findBySurveyorFiltered(
            Long surveyorId,
            SubmissionStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    // Surveyor — today's submissions
    Page<Submission> findTodayBySurveyor(
            Long surveyorId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            Pageable pageable
    );

    // Export (Excel/PDF) — no pagination
    List<Submission> findAllFilteredForExport(
            Long surveyorId,
            SubmissionStatus status,
            String serviceNumber,
            LocalDateTime from,
            LocalDateTime to
    );
}