package com.productapp.backend.repository;

import com.productapp.backend.entity.SubmissionImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionImageRepository extends JpaRepository<SubmissionImage, Long> {
    List<SubmissionImage> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}