package com.productapp.backend.service;

import com.productapp.backend.dto.AllowedEmailRequest;
import com.productapp.backend.dto.AllowedEmailResponse;
import com.productapp.backend.entity.AllowedEmail;
import com.productapp.backend.repository.AllowedEmailRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllowedEmailService {

    private final AllowedEmailRepository allowedEmailRepository;

    public List<AllowedEmailResponse> getAll() {
        return allowedEmailRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AllowedEmailResponse add(AllowedEmailRequest request) {
        // Idempotent — return existing if already present
        return allowedEmailRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .map(this::toResponse)
                .orElseGet(() -> {
                    AllowedEmail saved = allowedEmailRepository.save(
                            AllowedEmail.builder()
                                    .email(request.getEmail().toLowerCase().trim())
                                    .build());
                    log.info("Allowed email added: {}", saved.getEmail());
                    return toResponse(saved);
                });
    }

    public void remove(Long id) {
        if (!allowedEmailRepository.existsById(id)) {
            throw new EntityNotFoundException("Allowed email not found: " + id);
        }
        allowedEmailRepository.deleteById(id);
        log.info("Allowed email removed, id: {}", id);
    }

    private AllowedEmailResponse toResponse(AllowedEmail e) {
        return AllowedEmailResponse.builder()
                .id(e.getId())
                .email(e.getEmail())
                .createdAt(e.getCreatedAt())
                .build();
    }
}