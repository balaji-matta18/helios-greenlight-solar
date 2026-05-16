package com.productapp.backend.repository;

import com.productapp.backend.entity.Submission;
import com.productapp.backend.entity.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository
        extends JpaRepository<Submission, Long>, SubmissionRepositoryCustom {

    boolean existsByServiceNumber(String serviceNumber);

    Optional<Submission> findByServiceNumber(String serviceNumber);

    long countByStatus(SubmissionStatus status);

    // Surveyor — own submissions with optional filters
    @Query("""
            SELECT s FROM Submission s
            WHERE s.surveyor.id = :surveyorId
            AND (:status IS NULL OR s.status = :status)
            AND (:from IS NULL OR s.createdAt >= :from)
            AND (:to IS NULL OR s.createdAt <= :to)
            """)
    Page<Submission> findBySurveyorFiltered(
            @Param("surveyorId") Long surveyorId,
            @Param("status") SubmissionStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // Surveyor — today's submissions
    @Query("""
            SELECT s FROM Submission s
            WHERE s.surveyor.id = :surveyorId
            AND s.createdAt >= :startOfDay
            AND s.createdAt <= :endOfDay
            """)
    Page<Submission> findTodayBySurveyor(
            @Param("surveyorId") Long surveyorId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            Pageable pageable
    );

    // Admin — paginated list with filters
    // CAST(s.division AS string) forces Hibernate to emit ::text in PostgreSQL SQL,
    // which avoids the "function lower(bytea) does not exist" error on Neon.
    @Query("""
            SELECT s FROM Submission s
            WHERE (:surveyorId IS NULL OR s.surveyor.id = :surveyorId)
            AND (:status IS NULL OR s.status = :status)
            AND (:serviceNumber IS NULL OR s.serviceNumber = :serviceNumber)
            AND (:division IS NULL OR LOWER(CAST(s.division AS string)) LIKE LOWER(CONCAT('%', :division, '%')))
            AND (:from IS NULL OR s.createdAt >= :from)
            AND (:to IS NULL OR s.createdAt <= :to)
            """)
    Page<Submission> findAllFiltered(
            @Param("surveyorId") Long surveyorId,
            @Param("status") SubmissionStatus status,
            @Param("serviceNumber") String serviceNumber,
            @Param("division") String division,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

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