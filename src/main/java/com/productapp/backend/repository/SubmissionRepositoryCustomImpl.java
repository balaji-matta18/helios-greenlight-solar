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

@Repository
public class SubmissionRepositoryCustomImpl implements SubmissionRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    // FIX: uses CriteriaBuilder instead of JPQL for the admin paginated list.
    // JPQL with LOWER() on the division field fails on Neon with "function lower(bytea)
    // does not exist" regardless of CAST. CriteriaBuilder operates on the Java type
    // (String) from the entity mapping, so cb.lower() generates correct SQL every time.
    @Override
    public Page<Submission> findAllFilteredPaged(
            Long surveyorId,
            SubmissionStatus status,
            String serviceNumber,
            String division,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        // ── Data query ──────────────────────────────────────────────────────
        CriteriaQuery<Submission> dataQuery = cb.createQuery(Submission.class);
        Root<Submission> s = dataQuery.from(Submission.class);
        dataQuery.distinct(true);

        List<Predicate> predicates = buildPredicates(cb, s, surveyorId, status,
                serviceNumber, division, from, to);

        dataQuery.where(predicates.toArray(new Predicate[0]));
        dataQuery.orderBy(cb.desc(s.get("createdAt")));

        List<Submission> results = em.createQuery(dataQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // ── Count query (required for Page metadata) ─────────────────────
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Submission> cs = countQuery.from(Submission.class);

        List<Predicate> countPredicates = buildPredicates(cb, cs, surveyorId, status,
                serviceNumber, division, from, to);

        countQuery.select(cb.countDistinct(cs));
        countQuery.where(countPredicates.toArray(new Predicate[0]));

        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public List<Submission> findAllFilteredForExport(
            Long surveyorId,
            SubmissionStatus status,
            String serviceNumber,
            LocalDateTime from,
            LocalDateTime to) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Submission> cq = cb.createQuery(Submission.class);
        Root<Submission> s = cq.from(Submission.class);
        cq.distinct(true);

        List<Predicate> predicates = buildPredicates(cb, s, surveyorId, status,
                serviceNumber, null, from, to);

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(s.get("id")));

        return em.createQuery(cq).getResultList();
    }

    // ── Shared predicate builder ─────────────────────────────────────────────
    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            Root<Submission> s,
            Long surveyorId,
            SubmissionStatus status,
            String serviceNumber,
            String division,
            LocalDateTime from,
            LocalDateTime to) {

        List<Predicate> predicates = new ArrayList<>();

        if (surveyorId != null) {
            Join<Submission, Surveyor> surveyor = s.join("surveyor", JoinType.LEFT);
            predicates.add(cb.equal(surveyor.get("id"), surveyorId));
        }
        if (status != null) {
            predicates.add(cb.equal(s.get("status"), status));
        }
        if (serviceNumber != null && !serviceNumber.isBlank()) {
            predicates.add(cb.equal(s.get("serviceNumber"), serviceNumber));
        }
        if (division != null && !division.isBlank()) {
            // cb.lower() on a String entity field generates correct SQL —
            // no bytea ambiguity unlike JPQL LOWER() on Neon.
            predicates.add(cb.like(
                    cb.lower(s.get("division")),
                    "%" + division.toLowerCase() + "%"
            ));
        }
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(s.get("createdAt"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(s.get("createdAt"), to));
        }

        return predicates;
    }
}