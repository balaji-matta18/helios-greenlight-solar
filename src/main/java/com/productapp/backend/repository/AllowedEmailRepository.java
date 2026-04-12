package com.productapp.backend.repository;

import com.productapp.backend.entity.AllowedEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllowedEmailRepository extends JpaRepository<AllowedEmail, Long> {
    boolean existsByEmail(String email);
}