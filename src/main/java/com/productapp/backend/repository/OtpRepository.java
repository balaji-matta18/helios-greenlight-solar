package com.productapp.backend.repository;

import com.productapp.backend.entity.Otp;
import com.productapp.backend.entity.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByEmailAndOtpTypeOrderByCreatedAtDesc(String email, OtpType otpType);

    @Query("""
            SELECT COUNT(o) > 0 FROM Otp o
            WHERE o.email = :email
            AND o.otpType = :otpType
            AND o.createdAt >= :since
            """)
    boolean existsRecentOtp(
            @Param("email") String email,
            @Param("otpType") OtpType otpType,
            @Param("since") LocalDateTime since
    );
}