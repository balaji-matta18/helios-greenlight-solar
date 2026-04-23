package com.productapp.backend.repository;

import com.productapp.backend.entity.SubmissionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionAuditLogRepository extends JpaRepository<SubmissionAuditLog, Long> {

    List<SubmissionAuditLog> findBySubmissionIdOrderByEditedAtDesc(Long submissionId);
}