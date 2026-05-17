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

    private final SubmissionRepository         submissionRepository;
    private final SubmissionImageRepository    submissionImageRepository;
    private final SubmissionAuditLogRepository auditLogRepository;
    private final PanelNumberRepository        panelNumberRepository;
    private final SurveyorRepository           surveyorRepository;
    private final AdminRepository              adminRepository;
    private final S3Service                    s3Service;

    private static final List<String> ALLOWED_TYPES = List.of("image/png", "image/jpeg");
    private static final long MAX_SIZE = 5 * 1024 * 1024;


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
        log.info("Admin created submission: {}", saved.getServiceNumber());
        return mapToResponse(saved);
    }

    @Transactional
    public SubmissionResponse adminCreate(AdminSubmissionRequest request,
                                          Map<ImageType, MultipartFile> imageFiles) throws IOException {
        SubmissionResponse created = adminCreate(request);
        if (imageFiles != null && !imageFiles.isEmpty()) {
            Submission submission = getSubmissionById(created.getId());
            uploadImages(submission, imageFiles);
            return mapToResponse(submissionRepository.save(submission));
        }
        return created;
    }


    @Transactional
    public SubmissionResponse adminUpdate(Long id,
                                          AdminEditRequest request,
                                          Map<ImageType, MultipartFile> imageFiles) throws IOException {

        Submission submission = getSubmissionById(id);

        String editorEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        // Resolve admin's display name for consistent "Name (Admin)" format
        String editorName = adminRepository.findByEmail(editorEmail)
                .map(Admin::getUsername)
                .orElse(editorEmail);
        String editorLabel = editorName + " (Admin)";

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
        submission.setLastUpdatedBy(editorLabel);

        updatePanelNumbers(submission, request.getPanelNumber1(), request.getPanelNumber2(),
                request.getPanelNumber3(), request.getPanelNumber4());

        if (imageFiles != null && !imageFiles.isEmpty()) {
            uploadImages(submission, imageFiles);
        }

        Submission saved = submissionRepository.save(submission);

        auditLogRepository.save(SubmissionAuditLog.builder()
                .submission(saved)
                .editedByName(editorName)
                .editedByEmail(editorEmail)
                .editedByRole("ADMIN")
                .editNote(request.getEditNote())
                .build());

        log.info("Admin {} updated submission id: {}", editorEmail, id);
        return mapToResponse(saved);
    }


    @Transactional
    public void adminDelete(Long id) {
        Submission submission = getSubmissionById(id);
        deleteS3Images(submission);
        submissionRepository.delete(submission);
        log.info("Admin deleted submission id: {}", id);
    }


    @Transactional
    public void bulkCreate(List<AdminSubmissionRequest> requests) {
        for (AdminSubmissionRequest request : requests) {
            if (!submissionRepository.existsByServiceNumber(request.getServiceNumber())) {
                adminCreate(request);
            } else {
                log.warn("Skipped duplicate during bulk import: {}", request.getServiceNumber());
            }
        }
    }


    @Transactional(readOnly = true)
    public PageResponse<SubmissionSummaryResponse> adminGetAll(
            Long surveyorId, SubmissionStatus status, String division,
            String serviceNumber, LocalDateTime from, LocalDateTime to, Pageable pageable) {

        Page<Submission> page = submissionRepository.findAllFilteredPaged(
                surveyorId, status, serviceNumber, division, from, to, pageable);

        return buildPageResponse(page.map(this::mapToSummary));
    }


    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLog(Long submissionId) {
        getSubmissionById(submissionId);
        return auditLogRepository
                .findBySubmissionIdOrderByEditedAtDesc(submissionId)
                .stream()
                .map(a -> AuditLogResponse.builder()
                        .id(a.getId())
                        .editedByName(a.getEditedByName())
                        .editedByEmail(a.getEditedByEmail())
                        .editedByRole(a.getEditedByRole())
                        .editNote(a.getEditNote())
                        .editedAt(a.getEditedAt())
                        .build())
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public SubmissionResponse lookupByServiceNumber(String serviceNumber) {

        String currentEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Surveyor currentSurveyor = surveyorRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new SurveyorNotFoundException(currentEmail));

        Submission submission = submissionRepository
                .findByServiceNumber(serviceNumber)
                .orElseThrow(() -> new SubmissionNotFoundException(serviceNumber));

        if (submission.getSurveyor() != null &&
                !submission.getSurveyor().getId().equals(currentSurveyor.getId())) {
            throw new SubmissionNotFoundException(serviceNumber);
        }

        return mapToResponse(submission);
    }


    @Transactional
    public SubmissionResponse surveyorSubmit(SurveyorSubmitRequest request,
                                             Map<ImageType, MultipartFile> imageFiles) throws IOException {

        String currentEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Surveyor surveyor = surveyorRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new SurveyorNotFoundException(currentEmail));

        Submission submission = submissionRepository
                .findByServiceNumber(request.getServiceNumber())
                .orElseThrow(() -> new SubmissionNotFoundException(request.getServiceNumber()));

        if (submission.getStatus() == SubmissionStatus.SUBMITTED) {
            throw new SubmissionAlreadySubmittedException(request.getServiceNumber());
        }

        if (submission.getSurveyor() != null &&
                !submission.getSurveyor().getId().equals(surveyor.getId())) {
            throw new SubmissionNotFoundException(request.getServiceNumber());
        }

        String surveyorLabel = surveyor.getName() + " (Surveyor)";

        submission.setSurveyorName(request.getSurveyorName());
        submission.setSurveyor(surveyor);
        submission.setInverterSerialNumber(request.getInverterSerialNumber());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setLastUpdatedBy(surveyorLabel);

        updatePanelNumbers(submission, request.getPanelNumber1(), request.getPanelNumber2(),
                request.getPanelNumber3(), request.getPanelNumber4());

        if (imageFiles != null && !imageFiles.isEmpty()) {
            uploadImages(submission, imageFiles);
        }

        Submission saved = submissionRepository.save(submission);

        auditLogRepository.save(SubmissionAuditLog.builder()
                .submission(saved)
                .editedByName(surveyor.getName())
                .editedByEmail(currentEmail)
                .editedByRole("SURVEYOR")
                .editNote(null)
                .build());

        log.info("Surveyor {} submitted: {}", currentEmail, request.getServiceNumber());
        return mapToResponse(saved);
    }


    @Transactional
    public SubmissionResponse surveyorUpdate(Long id, SurveyorUpdateRequest request,
                                             Map<ImageType, MultipartFile> imageFiles) throws IOException {
        Submission submission = getSubmissionById(id);
        validateSurveyorOwnership(submission);

        String currentEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Surveyor surveyor = surveyorRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new SurveyorNotFoundException(currentEmail));

        String surveyorLabel = surveyor.getName() + " (Surveyor)";

        submission.setInverterSerialNumber(request.getInverterSerialNumber());
        submission.setUpdatedAt(LocalDateTime.now());
        submission.setLastUpdatedBy(surveyorLabel);

        updatePanelNumbers(submission, request.getPanelNumber1(), request.getPanelNumber2(),
                request.getPanelNumber3(), request.getPanelNumber4());

        if (imageFiles != null && !imageFiles.isEmpty()) {
            uploadImages(submission, imageFiles);
        }

        Submission saved = submissionRepository.save(submission);

        auditLogRepository.save(SubmissionAuditLog.builder()
                .submission(saved)
                .editedByName(surveyor.getName())
                .editedByEmail(currentEmail)
                .editedByRole("SURVEYOR")
                .editNote(null)
                .build());

        log.info("Surveyor {} updated submission id: {}", surveyor.getName(), id);
        return mapToResponse(saved);
    }


    @Transactional
    public void surveyorDelete(Long id) {
        Submission submission = getSubmissionById(id);
        validateSurveyorOwnership(submission);
        deleteS3Images(submission);
        submissionRepository.delete(submission);
        log.info("Surveyor deleted submission id: {}", id);
    }


    @Transactional(readOnly = true)
    public PageResponse<SubmissionSummaryResponse> surveyorGetOwn(
            SubmissionStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable) {

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Surveyor surveyor = surveyorRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new SurveyorNotFoundException(currentEmail));

        Page<Submission> page = submissionRepository.findBySurveyorFiltered(
                surveyor.getId(), status, from, to, pageable);

        return buildPageResponse(page.map(this::mapToSummary));
    }


    @Transactional(readOnly = true)
    public PageResponse<SubmissionSummaryResponse> surveyorGetToday(Pageable pageable) {

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Surveyor surveyor = surveyorRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new SurveyorNotFoundException(currentEmail));

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end   = LocalDate.now().atTime(LocalTime.MAX);

        Page<Submission> page = submissionRepository.findTodayBySurveyor(
                surveyor.getId(), start, end, pageable);

        return buildPageResponse(page.map(this::mapToSummary));
    }


    @Transactional(readOnly = true)
    public SubmissionResponse getById(Long id) {
        Submission submission = getSubmissionById(id);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSurveyor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SURVEYOR"));

        if (isSurveyor) {
            String currentEmail = auth.getName();
            if (submission.getSurveyor() == null ||
                    !submission.getSurveyor().getEmail().equals(currentEmail)) {
                throw new AccessDeniedException("This submission does not belong to you");
            }
        }

        return mapToResponse(submission);
    }


    private Submission getSubmissionById(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new SubmissionNotFoundException(String.valueOf(id)));
    }

    private void validateSurveyorOwnership(Submission submission) {
        String currentEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        if (submission.getSurveyor() == null ||
                !submission.getSurveyor().getEmail().equals(currentEmail)) {
            throw new AccessDeniedException("This submission does not belong to you");
        }
    }

    private void savePanelNumbers(Submission s, String p1, String p2, String p3, String p4) {
        List<String> panels = List.of(
                        p1 != null ? p1 : "",
                        p2 != null ? p2 : "",
                        p3 != null ? p3 : "",
                        p4 != null ? p4 : "")
                .stream()
                .filter(p -> !p.isBlank())
                .collect(Collectors.toList());

        List<PanelNumber> toSave = new ArrayList<>();
        for (int i = 0; i < panels.size(); i++) {
            toSave.add(PanelNumber.builder()
                    .panelNumber(panels.get(i))
                    .sequence(i + 1)
                    .submission(s)
                    .build());
        }
        panelNumberRepository.saveAll(toSave);
    }

    private void updatePanelNumbers(Submission s, String p1, String p2, String p3, String p4) {
        panelNumberRepository.deleteBySubmissionId(s.getId());
        savePanelNumbers(s, p1, p2, p3, p4);
    }

    private void uploadImages(Submission submission,
                              Map<ImageType, MultipartFile> imageFiles) throws IOException {
        for (Map.Entry<ImageType, MultipartFile> entry : imageFiles.entrySet()) {
            MultipartFile file = entry.getValue();
            if (file == null || file.isEmpty()) continue;
            if (!ALLOWED_TYPES.contains(file.getContentType()))
                throw new InvalidFileException("Only PNG and JPEG files are allowed");
            if (file.getSize() > MAX_SIZE)
                throw new InvalidFileException("File size exceeds 5MB limit");

            submissionImageRepository.findBySubmissionId(submission.getId())
                    .stream()
                    .filter(img -> img.getImageType() == entry.getKey())
                    .findFirst()
                    .ifPresent(existing -> {
                        s3Service.deleteFile(existing.getImageUrl());
                        submissionImageRepository.delete(existing);
                    });

            submissionImageRepository.save(SubmissionImage.builder()
                    .imageUrl(s3Service.uploadFile(file))
                    .imageType(entry.getKey())
                    .submission(submission)
                    .build());
        }
    }

    private void deleteS3Images(Submission submission) {
        submissionImageRepository.findBySubmissionId(submission.getId())
                .forEach(img -> s3Service.deleteFile(img.getImageUrl()));
    }

    private SubmissionResponse mapToResponse(Submission s) {
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
                .panelNumbers(panelNumberRepository
                        .findBySubmissionIdOrderBySequenceAsc(s.getId()).stream()
                        .map(p -> SubmissionResponse.PanelNumberDto.builder()
                                .sequence(p.getSequence()).panelNumber(p.getPanelNumber()).build())
                        .collect(Collectors.toList()))
                .images(submissionImageRepository.findBySubmissionId(s.getId()).stream()
                        .map(img -> SubmissionResponse.ImageDto.builder()
                                .id(img.getId()).imageType(img.getImageType().name())
                                .imageUrl(s3Service.generatePresignedUrl(img.getImageUrl())).build())
                        .collect(Collectors.toList()))
                .status(s.getStatus().name())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .lastUpdatedBy(s.getLastUpdatedBy())
                .build();
    }

    private SubmissionSummaryResponse mapToSummary(Submission s) {
        return SubmissionSummaryResponse.builder()
                .id(s.getId()).serviceNumber(s.getServiceNumber())
                .customerName(s.getCustomerName()).division(s.getDivision())
                .section(s.getSection()).surveyorName(s.getSurveyorName())
                .status(s.getStatus().name()).createdAt(s.getCreatedAt())
                .build();
    }

    private PageResponse<SubmissionSummaryResponse> buildPageResponse(
            Page<SubmissionSummaryResponse> page) {
        return PageResponse.<SubmissionSummaryResponse>builder()
                .content(page.getContent()).page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .last(page.isLast()).build();
    }
}