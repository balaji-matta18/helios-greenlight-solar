package com.productapp.backend.repository;

import com.productapp.backend.entity.Submission;
import com.productapp.backend.entity.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface SubmissionRepositoryCustom {

    // Used by admin dashboard — paginated with all filters including division
    Page<Submission> findAllFilteredPaged(
            Long surveyorId,
            SubmissionStatus status,
            String serviceNumber,
            String division,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    // Used by export (Excel/PDF) — no pagination needed
    List<Submission> findAllFilteredForExport(
            Long surveyorId,
            SubmissionStatus status,
            String serviceNumber,
            LocalDateTime from,
            LocalDateTime to
    );
}