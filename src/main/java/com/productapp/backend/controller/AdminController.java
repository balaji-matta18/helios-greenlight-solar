package com.productapp.backend.controller;

import com.productapp.backend.dto.*;
import com.productapp.backend.entity.ImageType;
import com.productapp.backend.entity.SubmissionStatus;
import com.productapp.backend.service.ExportService;
import com.productapp.backend.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SubmissionService submissionService;
    private final ExportService     exportService;

    @Operation(summary = "Create submission record")
    @PostMapping(value = "/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> create(
            @Valid @RequestPart("data") AdminSubmissionRequest request,
            @RequestPart(value = "panels_image",     required = false) MultipartFile panelsImage,
            @RequestPart(value = "inverter_image",   required = false) MultipartFile inverterImage,
            @RequestPart(value = "earth_image",      required = false) MultipartFile earthImage,
            @RequestPart(value = "bill_image",       required = false) MultipartFile billImage,
            @RequestPart(value = "aadhar_image",     required = false) MultipartFile aadharImage,
            @RequestPart(value = "document_image_1", required = false) MultipartFile docImage1,
            @RequestPart(value = "document_image_2", required = false) MultipartFile docImage2
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                submissionService.adminCreate(request,
                        buildImageMap(panelsImage, inverterImage, earthImage,
                                billImage, aadharImage, docImage1, docImage2)));
    }

    @Operation(summary = "Update submission — editNote field is mandatory")
    @PutMapping(value = "/submissions/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> update(
            @PathVariable Long id,
            @Valid @RequestPart("data") AdminEditRequest request,
            @RequestPart(value = "panels_image",     required = false) MultipartFile panelsImage,
            @RequestPart(value = "inverter_image",   required = false) MultipartFile inverterImage,
            @RequestPart(value = "earth_image",      required = false) MultipartFile earthImage,
            @RequestPart(value = "bill_image",       required = false) MultipartFile billImage,
            @RequestPart(value = "aadhar_image",     required = false) MultipartFile aadharImage,
            @RequestPart(value = "document_image_1", required = false) MultipartFile docImage1,
            @RequestPart(value = "document_image_2", required = false) MultipartFile docImage2
    ) throws IOException {
        return ResponseEntity.ok(
                submissionService.adminUpdate(id, request,
                        buildImageMap(panelsImage, inverterImage, earthImage,
                                billImage, aadharImage, docImage1, docImage2)));
    }

    @Operation(summary = "Approve a submitted submission")
    @PostMapping("/submissions/{id}/approve")
    public ResponseEntity<SubmissionResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.adminApprove(id));
    }

    @Operation(summary = "Reject a submitted submission — reason is mandatory")
    @PostMapping("/submissions/{id}/reject")
    public ResponseEntity<SubmissionResponse> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "").trim();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        return ResponseEntity.ok(submissionService.adminReject(id, reason));
    }

    @Operation(summary = "Delete submission")
    @DeleteMapping("/submissions/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        submissionService.adminDelete(id);
        return ResponseEntity.ok(new ApiResponse("Submission deleted successfully"));
    }

    @Operation(summary = "Pre-assign a surveyor to a PENDING submission")
    @PostMapping("/submissions/{id}/assign")
    public ResponseEntity<SubmissionResponse> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignSurveyorRequest request) {
        return ResponseEntity.ok(submissionService.assignSurveyor(id, request.getSurveyorId()));
    }

    @Operation(summary = "Remove pre-assignment from a PENDING submission")
    @DeleteMapping("/submissions/{id}/assign")
    public ResponseEntity<SubmissionResponse> unassign(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.unassignSurveyor(id));
    }

    @Operation(summary = "Get all submissions with filters")
    @GetMapping("/submissions")
    public ResponseEntity<PageResponse<SubmissionSummaryResponse>> getAll(
            @RequestParam(required = false) Long surveyorId,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) String division,
            @RequestParam(required = false) String serviceNumber,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean assigned,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return ResponseEntity.ok(
                submissionService.adminGetAll(surveyorId, status, division,
                        serviceNumber, search, assigned, from, to, pageable));
    }

    @Operation(summary = "Get single submission detail")
    @GetMapping("/submissions/{id}")
    public ResponseEntity<SubmissionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getById(id));
    }

    @Operation(summary = "Full edit audit log for a submission")
    @GetMapping("/submissions/{id}/audit")
    public ResponseEntity<List<AuditLogResponse>> getAuditLog(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getAuditLog(id));
    }

    @Operation(summary = "Bulk import submissions from Excel")
    @PostMapping(value = "/submissions/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> importExcel(
            @RequestPart("file") MultipartFile file) throws IOException {
        int count = exportService.importFromExcel(file);
        return ResponseEntity.ok(new ApiResponse(count + " records imported successfully"));
    }

    @Operation(summary = "Export as Excel")
    @GetMapping("/submissions/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Long surveyorId,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) String division,
            @RequestParam(required = false) String serviceNumber,
            @RequestParam(required = false) Boolean assigned,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) throws IOException {
        byte[] data = exportService.exportExcel(surveyorId, status, division, serviceNumber, assigned, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"submissions.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @Operation(summary = "Export as PDF")
    @GetMapping("/submissions/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) Long surveyorId,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) String division,
            @RequestParam(required = false) String serviceNumber,
            @RequestParam(required = false) Boolean assigned,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) throws IOException {
        byte[] data = exportService.exportPdf(surveyorId, status, division, serviceNumber, assigned, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"submissions.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    private Map<ImageType, MultipartFile> buildImageMap(
            MultipartFile panels, MultipartFile inverter, MultipartFile earth,
            MultipartFile bill,   MultipartFile aadhar,
            MultipartFile doc1,   MultipartFile doc2) {
        Map<ImageType, MultipartFile> map = new HashMap<>();
        if (panels   != null && !panels.isEmpty())   map.put(ImageType.PANELS,       panels);
        if (inverter != null && !inverter.isEmpty()) map.put(ImageType.INVERTER,     inverter);
        if (earth    != null && !earth.isEmpty())    map.put(ImageType.EARTH,        earth);
        if (bill     != null && !bill.isEmpty())     map.put(ImageType.CURRENT_BILL, bill);
        if (aadhar   != null && !aadhar.isEmpty())   map.put(ImageType.AADHAR,       aadhar);
        if (doc1     != null && !doc1.isEmpty())     map.put(ImageType.DOCUMENT_1,   doc1);
        if (doc2     != null && !doc2.isEmpty())     map.put(ImageType.DOCUMENT_2,   doc2);
        return map;
    }
}