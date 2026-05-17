package com.productapp.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submissions", indexes = {
        @Index(name = "idx_submission_surveyor", columnList = "surveyor_id"),
        @Index(name = "idx_submission_service_number", columnList = "service_number"),
        @Index(name = "idx_submission_created_at", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Admin-filled fields ──────────────────────────────
    @Column(name = "service_number", nullable = false, unique = true, length = 100)
    private String serviceNumber;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(length = 15)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String division;

    @Column(name = "sub_division", length = 100)
    private String subDivision;

    @Column(length = 100)
    private String section;

    @Column(length = 100)
    private String distribution;

    // ── Surveyor-filled fields ───────────────────────────
    @Column(name = "inverter_serial_number", length = 100)
    private String inverterSerialNumber;

    @Column(name = "surveyor_name", length = 100)
    private String surveyorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surveyor_id")
    private Surveyor surveyor;

    // ── Status ──────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    // ── Relations ───────────────────────────────────────
    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SubmissionImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PanelNumber> panelNumbers = new ArrayList<>();

    // ── Audit ────────────────────────────────────────────
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Stores name for surveyors, email for admins — whoever last touched this record
    @Column(name = "last_updated_by", length = 100)
    private String lastUpdatedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}