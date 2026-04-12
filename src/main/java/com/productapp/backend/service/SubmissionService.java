package com.productapp.backend.service;

import com.productapp.backend.dto.*;
import com.productapp.backend.entity.*;
import com.productapp.backend.exception.*;
import com.productapp.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionImageRepository submissionImageRepository;
    private final PanelNumberRepository panelNumberRepository;
    private final SurveyorRepository surveyorRepository;
    private final S3Service s3Service;

    private static final List<String> ALLOWED_TYPES = List.of("image/png", "image/jpeg");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    // ── Admin: create submission record ──────────────────────────────────────

    @Transactional
    public SubmissionResponse adminCreate(AdminSubmissionRequest request) {

        if (submissionRepository.existsByServiceNumber(request.getServiceNumber())) {
            throw new DuplicateServiceNumberException(request.getServiceNumber());
        }

        Submission submission = Submission.builder()
                .serviceNumber(request.getServiceNumber())
                .customerName(request.getCustomerName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .division(request.getDivision())
                .subDivision(request.getSubDivision())
                .section(request.getSection())
                .distribution(request.getDistribution())
                .inverterSerialNumber(request.getInverterSerialNumber())
                .surveyorName(request.getSurveyorName())
                .status(SubmissionStatus.PENDING)
                .build();

        Submission saved = submissionRepository.save(submission);
        savePanelNumbers(saved, request.getPanelNumber1(), request.getPanelNumber2(),
                request.getPanelNumber3(), request.getPanelNumber4());

        log.info("Admin created submission for service number: {}", saved.getServiceNumber());
        return mapToResponse(saved);
    }

    // ── Admin: update any field ───────────────────────────────────────────────

    @Transactional
    public SubmissionResponse adminUpdate(Long id, AdminSubmissionRequest request,
                                          Map<ImageType, MultipartFile> imageFiles) throws IOException {

        Submission submission = getSubmissionById(id);

        submission.setServiceNumber(request.getServiceNumber());
        submission.setCustomerName(request.getCustomerName());
        submission.setPhone(request.getPhone());
        submission.setAddress(request.getAddress());
        submission.setDivision(request.getDivision());
        submission.setSubDivision(request.getSubDivision());
        submission.setSection(request.getSection());
        submission.setDistribution(request.getDistribution());
        submission.setInverterSerialNumber(request.getInverterSerialNumber());
        submission.setSurveyorName(request.getSurveyorName());

        updatePanelNumbers(submission, request.getPanelNumber1(), request.getPanelNumber2(),
                request.getPanelNumber3(), request.getPanelNumber4());

        if (imageFiles != null && !imageFiles.isEmpty()) {
            uploadImages(submission, imageFiles);
        }

        Submission saved = submissionRepository.save(submission);
        log.info("Admin updated submission id: {}", id);
        return mapToResponse(saved);
    }

    // ── Admin: delete submission ──────────────────────────────────────────────

    @Transactional
    public void adminDelete(Long id) {
        Submission submission = getSubmissionById(id);
        deleteS3Images(submission);
        submissionRepository.delete(submission);
        log.info("Admin deleted submission id: {}", id);
    }

    // ── Admin: bulk import from Excel (called from ExportService) ────────────

    @Transactional
    public void bulkCreate(List<AdminSubmissionRequest> requests) {
        for (AdminSubmissionRequest request : requests) {
            if (!submissionRepository.existsByServiceNumber(request.getServiceNumber())) {
                adminCreate(request);
            } else {
                log.warn("Skipped duplicate service number during bulk import: {}",
                        request.getServiceNumber());
            }
        }
    }

    // ── Admin: get all with filters ───────────────────────────────────────────

    public PageResponse<SubmissionSummaryResponse> adminGetAll(
            Long surveyorId, SubmissionStatus status, String division,
            String section, LocalDateTime from, LocalDateTime to, Pageable pageable) {

        Pageable safePage = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 50),
                pageable.getSort()
        );

        Page<SubmissionSummaryResponse> page = submissionRepository
                .findAllFiltered(surveyorId, status, division, section, from, to, safePage)
                .map(this::mapToSummary);

        return buildPageResponse(page);
    }

    // ── Surveyor: lookup by service number ────────────────────────────────────

    public SubmissionResponse lookupByServiceNumber(String serviceNumber) {
        Submission submission = submissionRepository.findByServiceNumber(serviceNumber)
                .orElseThrow(() -> new SubmissionNotFoundException(serviceNumber));
        return mapToResponse(submission);
    }

    // ── Surveyor: submit (complete the form) ──────────────────────────────────

    @Transactional
    public SubmissionResponse surveyorSubmit(SurveyorSubmitRequest request,
                                             Map<ImageType, MultipartFile> imageFiles) throws IOException {

        Submission submission = submissionRepository
                .findByServiceNumber(request.getServiceNumber())
                .orElseThrow(() -> new SubmissionNotFoundException(request.getServiceNumber()));

        if (submission.getStatus() == SubmissionStatus.SUBMITTED) {
            throw new SubmissionAlreadySubmittedException(request.getServiceNumber());
        }

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Surveyor surveyor = surveyorRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new SurveyorNotFoundException(currentEmail));

        submission.setSurveyorName(request.getSurveyorName());
        submission.setSurveyor(surveyor);
        submission.setInverterSerialNumber(request.getInverterSerialNumber());
        submission.setStatus(SubmissionStatus.SUBMITTED);

        updatePanelNumbers(submission, request.getPanelNumber1(), request.getPanelNumber2(),
                request.getPanelNumber3(), request.getPanelNumber4());

        if (imageFiles != null && !imageFiles.isEmpty()) {
            uploadImages(submission, imageFiles);
        }

        Submission saved = submissionRepository.save(submission);
        log.info("Surveyor {} submitted service number: {}", currentEmail, request.getServiceNumber());
        return mapToResponse(saved);
    }

    // ── Surveyor: edit after submission ───────────────────────────────────────

    @Transactional
    public SubmissionResponse surveyorUpdate(Long id, SurveyorUpdateRequest request,
                                             Map<ImageType, MultipartFile> imageFiles) throws IOException {

        Submission submission = getSubmissionById(id);
        validateSurveyorOwnership(submission);

        // surveyor name intentionally NOT updated here
        submission.setInverterSerialNumber(request.getInverterSerialNumber());

        updatePanelNumbers(submission, request.getPanelNumber1(), request.getPanelNumber2(),
                request.getPanelNumber3(), request.getPanelNumber4());

        if (imageFiles != null && !imageFiles.isEmpty()) {
            uploadImages(submission, imageFiles);
        }

        Submission saved = submissionRepository.save(submission);
        log.info("Surveyor updated submission id: {}", id);
        return mapToResponse(saved);
    }

    // ── Surveyor: delete ──────────────────────────────────────────────────────

    @Transactional
    public void surveyorDelete(Long id) {
        Submission submission = getSubmissionById(id);
        validateSurveyorOwnership(submission);
        deleteS3Images(submission);
        submissionRepository.delete(submission);
        log.info("Surveyor deleted submission id: {}", id);
    }

    // ── Surveyor: own submissions with filters ────────────────────────────────

    public PageResponse<SubmissionSummaryResponse> surveyorGetOwn(
            SubmissionStatus status, LocalDateTime from,
            LocalDateTime to, Pageable pageable) {

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Surveyor surveyor = surveyorRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new SurveyorNotFoundException(currentEmail));

        Pageable safePage = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 50),
                pageable.getSort()
        );

        Page<SubmissionSummaryResponse> page = submissionRepository
                .findBySurveyorFiltered(surveyor.getId(), status, from, to, safePage)
                .map(this::mapToSummary);

        return buildPageResponse(page);
    }

    // ── Surveyor: today's submissions ─────────────────────────────────────────

    public PageResponse<SubmissionSummaryResponse> surveyorGetToday(Pageable pageable) {

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Surveyor surveyor = surveyorRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new SurveyorNotFoundException(currentEmail));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        Pageable safePage = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 50),
                pageable.getSort()
        );

        Page<SubmissionSummaryResponse> page = submissionRepository
                .findTodayBySurveyor(surveyor.getId(), startOfDay, endOfDay, safePage)
                .map(this::mapToSummary);

        return buildPageResponse(page);
    }

    // ── Shared: get single submission ─────────────────────────────────────────

    public SubmissionResponse getById(Long id) {
        return mapToResponse(getSubmissionById(id));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Submission getSubmissionById(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new SubmissionNotFoundException(String.valueOf(id)));
    }

    private void validateSurveyorOwnership(Submission submission) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (submission.getSurveyor() == null ||
                !submission.getSurveyor().getEmail().equals(currentEmail)) {
            throw new AccessDeniedException("This submission does not belong to you");
        }
    }

    private void savePanelNumbers(Submission submission, String p1, String p2, String p3, String p4) {
        List<String> panels = List.of(
                p1 != null ? p1 : "",
                p2 != null ? p2 : "",
                p3 != null ? p3 : "",
                p4 != null ? p4 : ""
        );
        for (int i = 0; i < panels.size(); i++) {
            if (!panels.get(i).isBlank()) {
                PanelNumber pn = PanelNumber.builder()
                        .panelNumber(panels.get(i))
                        .sequence(i + 1)
                        .submission(submission)
                        .build();
                panelNumberRepository.save(pn);
            }
        }
    }

    private void updatePanelNumbers(Submission submission, String p1, String p2, String p3, String p4) {
        panelNumberRepository.deleteBySubmissionId(submission.getId());
        savePanelNumbers(submission, p1, p2, p3, p4);
    }

    private void uploadImages(Submission submission,
                              Map<ImageType, MultipartFile> imageFiles) throws IOException {
        for (Map.Entry<ImageType, MultipartFile> entry : imageFiles.entrySet()) {
            MultipartFile file = entry.getValue();
            if (file == null || file.isEmpty()) continue;

            if (!ALLOWED_TYPES.contains(file.getContentType())) {
                throw new InvalidFileException("Only PNG and JPEG files are allowed");
            }
            if (file.getSize() > MAX_SIZE) {
                throw new InvalidFileException("File size exceeds 5MB limit");
            }

            // Replace existing image of same type if present
            submissionImageRepository.findBySubmissionId(submission.getId())
                    .stream()
                    .filter(img -> img.getImageType() == entry.getKey())
                    .findFirst()
                    .ifPresent(existing -> {
                        s3Service.deleteFile(existing.getImageUrl());
                        submissionImageRepository.delete(existing);
                    });

            String url = s3Service.uploadFile(file);
            SubmissionImage image = SubmissionImage.builder()
                    .imageUrl(url)
                    .imageType(entry.getKey())
                    .submission(submission)
                    .build();
            submissionImageRepository.save(image);
        }
    }

    private void deleteS3Images(Submission submission) {
        submissionImageRepository.findBySubmissionId(submission.getId())
                .forEach(img -> s3Service.deleteFile(img.getImageUrl()));
    }

    private SubmissionResponse mapToResponse(Submission s) {
        List<SubmissionResponse.PanelNumberDto> panels =
                panelNumberRepository.findBySubmissionIdOrderBySequenceAsc(s.getId())
                        .stream()
                        .map(p -> SubmissionResponse.PanelNumberDto.builder()
                                .sequence(p.getSequence())
                                .panelNumber(p.getPanelNumber())
                                .build())
                        .collect(Collectors.toList());

        List<SubmissionResponse.ImageDto> images =
                submissionImageRepository.findBySubmissionId(s.getId())
                        .stream()
                        .map(img -> SubmissionResponse.ImageDto.builder()
                                .id(img.getId())
                                .imageType(img.getImageType().name())
                                .imageUrl(img.getImageUrl())
                                .build())
                        .collect(Collectors.toList());

        return SubmissionResponse.builder()
                .id(s.getId())
                .serviceNumber(s.getServiceNumber())
                .customerName(s.getCustomerName())
                .phone(s.getPhone())
                .address(s.getAddress())
                .division(s.getDivision())
                .subDivision(s.getSubDivision())
                .section(s.getSection())
                .distribution(s.getDistribution())
                .inverterSerialNumber(s.getInverterSerialNumber())
                .surveyorName(s.getSurveyorName())
                .surveyorEmail(s.getSurveyor() != null ? s.getSurveyor().getEmail() : null)
                .panelNumbers(panels)
                .images(images)
                .status(s.getStatus().name())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private SubmissionSummaryResponse mapToSummary(Submission s) {
        return SubmissionSummaryResponse.builder()
                .id(s.getId())
                .serviceNumber(s.getServiceNumber())
                .customerName(s.getCustomerName())
                .division(s.getDivision())
                .section(s.getSection())
                .surveyorName(s.getSurveyorName())
                .status(s.getStatus().name())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private PageResponse<SubmissionSummaryResponse> buildPageResponse(
            Page<SubmissionSummaryResponse> page) {
        return PageResponse.<SubmissionSummaryResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}