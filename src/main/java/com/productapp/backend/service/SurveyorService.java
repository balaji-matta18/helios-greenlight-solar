package com.productapp.backend.service;

import com.productapp.backend.dto.SurveyorSignupRequest;
import com.productapp.backend.entity.Surveyor;
import com.productapp.backend.exception.EmailAlreadyRegisteredException;
import com.productapp.backend.exception.EmailNotAllowedException;
import com.productapp.backend.repository.AllowedEmailRepository;
import com.productapp.backend.repository.SurveyorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyorService {

    private final SurveyorRepository surveyorRepository;
    private final AllowedEmailRepository allowedEmailRepository;

    public void signup(SurveyorSignupRequest request) {

        if (!allowedEmailRepository.existsByEmail(request.getEmail())) {
            log.warn("Signup rejected — email not in whitelist: {}", request.getEmail());
            throw new EmailNotAllowedException(request.getEmail());
        }

        if (surveyorRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException(request.getEmail());
        }

        Surveyor surveyor = Surveyor.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();

        surveyorRepository.save(surveyor);
        log.info("New surveyor registered: {}", request.getEmail());
    }

    public Surveyor getByEmail(String email) {
        return surveyorRepository.findByEmail(email)
                .orElseThrow(() -> new com.productapp.backend.exception.SurveyorNotFoundException(email));
    }
}