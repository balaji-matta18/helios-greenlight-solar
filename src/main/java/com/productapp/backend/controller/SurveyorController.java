package com.productapp.backend.controller;

import com.productapp.backend.dto.*;
import com.productapp.backend.entity.ImageType;
import com.productapp.backend.entity.SubmissionStatus;
import com.productapp.backend.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/surveyor")
@RequiredArgsConstructor
public class SurveyorController {

    private final SubmissionService submissionService;

    @Operation(summary = "Lookup submission by service number")
    @GetMapping("/submissions/lookup")
    public ResponseEntity<SubmissionResponse> lookup(@RequestParam String serviceNumber) {
        return ResponseEntity.ok(submissionService.lookupByServiceNumber(serviceNumber));
    }

    @Operation(summary = "Submit completed form")
    @PostMapping(value = "/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> submit(
            @Valid @RequestPart("data") SurveyorSubmitRequest request,
            @RequestPart(value = "panels_image", required = false) MultipartFile panelsImage,
            @RequestPart(value = "inverter_image", required = false) MultipartFile inverterImage,
            @RequestPart(value = "earth_image", required = false) MultipartFile earthImage,
            @RequestPart(value = "bill_image", required = false) MultipartFile billImage,
            @RequestPart(value = "aadhar_image", required = false) MultipartFile aadharImage,
            @RequestPart(value = "document_image_1", required = false) MultipartFile docImage1,
            @RequestPart(value = "document_image_2", required = false) MultipartFile docImage2
    ) throws IOException {
        return ResponseEntity.ok(
                submissionService.surveyorSubmit(request, buildImageMap(
                        panelsImage, inverterImage, earthImage,
                        billImage, aadharImage, docImage1, docImage2))
        );
    }

    @Operation(summary = "Edit own submission")
    @PutMapping(value = "/submissions/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> update(
            @PathVariable Long id,
            @RequestPart("data") SurveyorUpdateRequest request,
            @RequestPart(value = "panels_image", required = false) MultipartFile panelsImage,
            @RequestPart(value = "inverter_image", required = false) MultipartFile inverterImage,
            @RequestPart(value = "earth_image", required = false) MultipartFile earthImage,
            @RequestPart(value = "bill_image", required = false) MultipartFile billImage,
            @RequestPart(value = "aadhar_image", required = false) MultipartFile aadharImage,
            @RequestPart(value = "document_image_1", required = false) MultipartFile docImage1,
            @RequestPart(value = "document_image_2", required = false) MultipartFile docImage2
    ) throws IOException {
        return ResponseEntity.ok(
                submissionService.surveyorUpdate(id, request, buildImageMap(
                        panelsImage, inverterImage, earthImage,
                        billImage, aadharImage, docImage1, docImage2))
        );
    }

    @Operation(summary = "Delete own submission")
    @DeleteMapping("/submissions/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) throws IOException {
        submissionService.surveyorDelete(id);
        return ResponseEntity.ok(new ApiResponse("Submission deleted successfully"));
    }

    @Operation(summary = "Get own submissions with filters")
    @GetMapping("/submissions")
    public ResponseEntity<PageResponse<SubmissionSummaryResponse>> getOwn(
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return ResponseEntity.ok(submissionService.surveyorGetOwn(status, from, to, pageable));
    }

    @Operation(summary = "Get today's submissions")
    @GetMapping("/submissions/today")
    public ResponseEntity<PageResponse<SubmissionSummaryResponse>> getToday(Pageable pageable) {
        return ResponseEntity.ok(submissionService.surveyorGetToday(pageable));
    }

    @Operation(summary = "Get PENDING submissions pre-assigned to the current surveyor")
    @GetMapping("/submissions/assigned")
    public ResponseEntity<PageResponse<SubmissionSummaryResponse>> getAssigned(Pageable pageable) {
        return ResponseEntity.ok(submissionService.getAssignedToMe(pageable));
    }

    @Operation(summary = "Get single submission detail")
    @GetMapping("/submissions/{id}")
    public ResponseEntity<SubmissionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getById(id));
    }

    private Map<ImageType, MultipartFile> buildImageMap(
            MultipartFile panels, MultipartFile inverter, MultipartFile earth,
            MultipartFile bill, MultipartFile aadhar,
            MultipartFile doc1, MultipartFile doc2) {
        Map<ImageType, MultipartFile> map = new HashMap<>();
        if (panels != null && !panels.isEmpty()) map.put(ImageType.PANELS, panels);
        if (inverter != null && !inverter.isEmpty()) map.put(ImageType.INVERTER, inverter);
        if (earth != null && !earth.isEmpty()) map.put(ImageType.EARTH, earth);
        if (bill != null && !bill.isEmpty()) map.put(ImageType.CURRENT_BILL, bill);
        if (aadhar != null && !aadhar.isEmpty()) map.put(ImageType.AADHAR, aadhar);
        if (doc1 != null && !doc1.isEmpty()) map.put(ImageType.DOCUMENT_1, doc1);
        if (doc2 != null && !doc2.isEmpty()) map.put(ImageType.DOCUMENT_2, doc2);
        return map;
    }
}