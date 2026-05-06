package com.productapp.backend.repository;

import com.productapp.backend.entity.Submission;
import com.productapp.backend.entity.SubmissionStatus;
import com.productapp.backend.entity.Surveyor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Repository
public class SubmissionRepositoryCustomImpl implements SubmissionRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

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
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(s.get("createdAt"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(s.get("createdAt"), to));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(s.get("id")));

        return em.createQuery(cq).getResultList();
    }
}