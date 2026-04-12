package com.productapp.backend.repository;

import com.productapp.backend.entity.PanelNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PanelNumberRepository extends JpaRepository<PanelNumber, Long> {
    List<PanelNumber> findBySubmissionIdOrderBySequenceAsc(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}