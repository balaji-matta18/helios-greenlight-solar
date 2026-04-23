package com.productapp.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "submission_audit_log", indexes = {
        @Index(name = "idx_audit_submission", columnList = "submission_id"),
        @Index(name = "idx_audit_edited_at",  columnList = "edited_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubmissionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    // Trusted — taken from admin JWT token, never from input
    @Column(name = "edited_by_email", nullable = false, length = 100)
    private String editedByEmail;

    // Typed by the admin — what they changed / why
    @Column(name = "edit_note", nullable = false, length = 500)
    private String editNote;

    @Column(name = "edited_at", nullable = false, updatable = false)
    private LocalDateTime editedAt;

    @PrePersist
    protected void onCreate() {
        this.editedAt = LocalDateTime.now();
    }
}