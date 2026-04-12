package com.productapp.backend.repository;

import com.productapp.backend.entity.Surveyor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurveyorRepository extends JpaRepository<Surveyor, Long> {
    Optional<Surveyor> findByEmail(String email);
    boolean existsByEmail(String email);
}