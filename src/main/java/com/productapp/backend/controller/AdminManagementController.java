package com.productapp.backend.controller;

import com.productapp.backend.dto.*;
import com.productapp.backend.service.AllowedEmailService;
import com.productapp.backend.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminManagementController {

    private final StatsService statsService;
    private final AllowedEmailService allowedEmailService;

    @Operation(summary = "Dashboard stats — total, pending, submitted, surveyorCount")
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(statsService.getDashboardStats());
    }

    @Operation(summary = "Submissions per day — last 7 days for bar chart")
    @GetMapping("/stats/weekly")
    public ResponseEntity<WeeklyStatsResponse> getWeeklyStats() {
        return ResponseEntity.ok(statsService.getWeeklyStats());
    }

    @Operation(summary = "All registered surveyors with submission counts")
    @GetMapping("/surveyors")
    public ResponseEntity<List<SurveyorListResponse>> getSurveyors() {
        return ResponseEntity.ok(statsService.getSurveyorList());
    }

    @Operation(summary = "Get all whitelisted emails")
    @GetMapping("/allowed-emails")
    public ResponseEntity<List<AllowedEmailResponse>> getAllowedEmails() {
        return ResponseEntity.ok(allowedEmailService.getAll());
    }

    @Operation(summary = "Add email to whitelist")
    @PostMapping("/allowed-emails")
    public ResponseEntity<AllowedEmailResponse> addAllowedEmail(
            @Valid @RequestBody AllowedEmailRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(allowedEmailService.add(request));
    }

    @Operation(summary = "Remove email from whitelist")
    @DeleteMapping("/allowed-emails/{id}")
    public ResponseEntity<ApiResponse> removeAllowedEmail(@PathVariable Long id) {
        allowedEmailService.remove(id);
        return ResponseEntity.ok(new ApiResponse("Email removed from whitelist"));
    }
}