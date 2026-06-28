package com.productapp.backend.repository;

import com.productapp.backend.entity.Submission;
import com.productapp.backend.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * All dynamic filtered queries are handled by SubmissionRepositoryCustomImpl
 * using CriteriaBuilder, which avoids Neon PostgreSQL's inability to infer
 * the data type of null parameters in JPQL "(:param IS NULL OR ...)" patterns.
 *
 * Only simple non-nullable queries remain here as JPQL.
 */
public interface SubmissionRepository
        extends JpaRepository<Submission, Long>, SubmissionRepositoryCustom {

    boolean existsByServiceNumber(String serviceNumber);

    Optional<Submission> findByServiceNumber(String serviceNumber);

    long countByStatus(SubmissionStatus status);

    long countByStatusAndSurveyorIsNull(SubmissionStatus status);

    @Query("""
            SELECT CAST(s.createdAt AS LocalDate), COUNT(s)
            FROM Submission s
            WHERE s.createdAt >= :since
            GROUP BY CAST(s.createdAt AS LocalDate)
            ORDER BY CAST(s.createdAt AS LocalDate) ASC
            """)
    List<Object[]> countByDaySince(@Param("since") LocalDateTime since);

    @Query("""
            SELECT s.surveyor.id, COUNT(s)
            FROM Submission s
            WHERE s.surveyor IS NOT NULL
            GROUP BY s.surveyor.id
            """)
    List<Object[]> countBySurveyor();
}