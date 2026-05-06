package com.productapp.backend.repository;

import com.productapp.backend.entity.Submission;
import com.productapp.backend.entity.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface SubmissionRepositoryCustom {

    List<Submission> findAllFilteredForExport(
            Long surveyorId,
            SubmissionStatus status,
            String serviceNumber,
            LocalDateTime from,
            LocalDateTime to
    );
}