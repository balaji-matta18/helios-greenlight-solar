package com.productapp.backend.repository;

import com.productapp.backend.entity.AllowedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AllowedEmailRepository extends JpaRepository<AllowedEmail, Long> {
    boolean existsByEmail(String email);
    Optional<AllowedEmail> findByEmail(String email);
}