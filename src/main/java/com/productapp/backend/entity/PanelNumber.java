package com.productapp.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "panel_numbers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PanelNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "panel_number", nullable = false, length = 100)
    private String panelNumber;

    @Column(nullable = false)
    private Integer sequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;
}