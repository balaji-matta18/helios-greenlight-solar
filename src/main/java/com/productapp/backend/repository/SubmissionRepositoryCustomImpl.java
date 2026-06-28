package com.productapp.backend.repository;

import com.productapp.backend.entity.Submission;
import com.productapp.backend.entity.SubmissionStatus;
import com.productapp.backend.entity.Surveyor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * All dynamic queries use CriteriaBuilder instead of JPQL.
 *
 * Reason: Neon PostgreSQL cannot determine the data type of null parameters
 * in JPQL patterns like "(:param IS NULL OR col = :param)". This causes
 * "could not determine data type of parameter $N" errors at runtime.
 * CriteriaBuilder operates on Java types from the entity mapping and only
 * adds predicates when values are non-null, so it never sends untyped nulls.
 */
@Repository
public class SubmissionRepositoryCustomImpl implements SubmissionRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    // ── Admin paginated list ─────────────────────────────────────────────────

    @Override
    public Page<Submission> findAllFilteredPaged(
            Long surveyorId, SubmissionStatus status, String serviceNumber,
            String division, String searchQuery, Boolean assigned, LocalDateTime from, LocalDateTime to, Pageable pageable) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Data query
        CriteriaQuery<Submission> dataQuery = cb.createQuery(Submission.class);
        Root<Submission> s = dataQuery.from(Submission.class);
        dataQuery.distinct(true);
        dataQuery.where(buildAdminPredicates(cb, s, surveyorId, status, serviceNumber, division, searchQuery, assigned, from, to));
        dataQuery.orderBy(cb.desc(s.get("createdAt")));

        List<Submission> results = em.createQuery(dataQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Submission> cs = countQuery.from(Submission.class);
        countQuery.select(cb.countDistinct(cs));
        countQuery.where(buildAdminPredicates(cb, cs, surveyorId, status, serviceNumber, division, searchQuery, assigned, from, to));
        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    // ── Surveyor — own submissions ───────────────────────────────────────────

    @Override
    public Page<Submission> findBySurveyorFiltered(
            Long surveyorId, SubmissionStatus status,
            LocalDateTime from, LocalDateTime to, Pageable pageable) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Submission> dataQuery = cb.createQuery(Submission.class);
        Root<Submission> s = dataQuery.from(Submission.class);
        dataQuery.where(buildSurveyorPredicates(cb, s, surveyorId, status, from, to));
        dataQuery.orderBy(cb.desc(s.get("createdAt")));

        List<Submission> results = em.createQuery(dataQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Submission> cs = countQuery.from(Submission.class);
        countQuery.select(cb.count(cs));
        countQuery.where(buildSurveyorPredicates(cb, cs, surveyorId, status, from, to));
        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    // ── Surveyor — today's submissions ──────────────────────────────────────

    @Override
    public Page<Submission> findTodayBySurveyor(
            Long surveyorId, LocalDateTime startOfDay, LocalDateTime endOfDay, Pageable pageable) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Submission> dataQuery = cb.createQuery(Submission.class);
        Root<Submission> s = dataQuery.from(Submission.class);

        Predicate[] predicates = {
                cb.equal(s.get("surveyor").get("id"), surveyorId),
                cb.greaterThanOrEqualTo(s.get("createdAt"), startOfDay),
                cb.lessThanOrEqualTo(s.get("createdAt"), endOfDay)
        };
        dataQuery.where(predicates);
        dataQuery.orderBy(cb.desc(s.get("createdAt")));

        List<Submission> results = em.createQuery(dataQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Submission> cs = countQuery.from(Submission.class);
        countQuery.select(cb.count(cs));
        countQuery.where(
                cb.equal(cs.get("surveyor").get("id"), surveyorId),
                cb.greaterThanOrEqualTo(cs.get("createdAt"), startOfDay),
                cb.lessThanOrEqualTo(cs.get("createdAt"), endOfDay)
        );
        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    // ── Export ───────────────────────────────────────────────────────────────

    @Override
    public List<Submission> findAllFilteredForExport(
            Long surveyorId, SubmissionStatus status, String serviceNumber,
            String division, String searchQuery, Boolean assigned, LocalDateTime from, LocalDateTime to) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Submission> cq = cb.createQuery(Submission.class);
        Root<Submission> s = cq.from(Submission.class);
        cq.distinct(true);
        cq.where(buildAdminPredicates(cb, s, surveyorId, status, serviceNumber, division, searchQuery, assigned, from, to));
        cq.orderBy(cb.asc(s.get("id")));

        return em.createQuery(cq).getResultList();
    }

    // ── Predicate builders ───────────────────────────────────────────────────

    private Predicate[] buildAdminPredicates(
            CriteriaBuilder cb, Root<Submission> s,
            Long surveyorId, SubmissionStatus status, String serviceNumber,
            String division, String searchQuery, Boolean assigned, LocalDateTime from, LocalDateTime to) {

        List<Predicate> predicates = new ArrayList<>();

        if (surveyorId != null) {
            Join<Submission, Surveyor> surveyor = s.join("surveyor", JoinType.LEFT);
            predicates.add(cb.equal(surveyor.get("id"), surveyorId));
        } else if (assigned != null) {
            Join<Submission, Surveyor> surveyor = s.join("surveyor", JoinType.LEFT);
            if (assigned) {
                predicates.add(cb.isNotNull(surveyor.get("id")));
            } else {
                predicates.add(cb.isNull(surveyor.get("id")));
            }
        }
        if (status != null) {
            predicates.add(cb.equal(s.get("status"), status));
        }
        if (serviceNumber != null && !serviceNumber.isBlank()) {
            predicates.add(cb.equal(s.get("serviceNumber"), serviceNumber));
        }
        if (division != null && !division.isBlank()) {
            predicates.add(cb.like(
                    cb.lower(s.get("division")),
                    "%" + division.toLowerCase() + "%"
            ));
        }
        if (searchQuery != null && !searchQuery.isBlank()) {
            String pattern = "%" + searchQuery.toLowerCase() + "%";
            Join<Submission, Surveyor> surveyor = s.join("surveyor", JoinType.LEFT);
            predicates.add(cb.or(
                    cb.like(cb.lower(s.get("serviceNumber")), pattern),
                    cb.like(cb.lower(s.get("customerName")), pattern),
                    cb.like(cb.lower(s.get("division")), pattern),
                    cb.like(cb.lower(surveyor.get("name")), pattern)
            ));
        }
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(s.get("createdAt"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(s.get("createdAt"), to));
        }

        return predicates.toArray(new Predicate[0]);
    }

    private Predicate[] buildSurveyorPredicates(
            CriteriaBuilder cb, Root<Submission> s,
            Long surveyorId, SubmissionStatus status,
            LocalDateTime from, LocalDateTime to) {

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(s.get("surveyor").get("id"), surveyorId));

        if (status != null) {
            predicates.add(cb.equal(s.get("status"), status));
        }
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(s.get("createdAt"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(s.get("createdAt"), to));
        }

        return predicates.toArray(new Predicate[0]);
    }
}