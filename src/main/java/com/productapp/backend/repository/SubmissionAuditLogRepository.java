package com.productapp.backend.repository;

import com.productapp.backend.entity.SubmissionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionAuditLogRepository extends JpaRepository<SubmissionAuditLog, Long> {

    List<SubmissionAuditLog> findBySubmissionIdOrderByEditedAtDesc(Long submissionId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT CAST(a.editedAt AS LocalDate), COUNT(DISTINCT a.submission.id)
            FROM SubmissionAuditLog a
            WHERE a.editedAt >= :since AND a.editedByRole = 'SURVEYOR'
            GROUP BY CAST(a.editedAt AS LocalDate)
            ORDER BY CAST(a.editedAt AS LocalDate) ASC
            """)
    List<Object[]> countSurveyorSubmissionsByDaySince(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
}