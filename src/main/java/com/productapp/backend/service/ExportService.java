package com.productapp.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.productapp.backend.dto.AdminSubmissionRequest;
import com.productapp.backend.entity.*;
import com.productapp.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final SubmissionRepository submissionRepository;
    private final PanelNumberRepository panelNumberRepository;
    private final SubmissionService submissionService;

    private static final String[] HEADERS = {
            "ID", "Service Number", "Customer Name", "Phone", "Address",
            "Division", "Sub Division", "Section", "Distribution",
            "Inverter Serial No.", "Panel 1", "Panel 2", "Panel 3", "Panel 4",
            "Surveyor Name", "Surveyor Email", "Status", "Created At"
    };

    // ── Excel export ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportExcel(Long surveyorId, SubmissionStatus status,
                              String division, String serviceNumber,
                              LocalDateTime from, LocalDateTime to) throws IOException {

        List<Submission> submissions = submissionRepository
                .findAllFilteredForExport(surveyorId, status, serviceNumber, from, to);

        // Case-insensitive division filter applied in Java (avoids LOWER(bytea) PostgreSQL type error)
        if (division != null && !division.isBlank()) {
            final String divLower = division.toLowerCase();
            submissions = submissions.stream()
                    .filter(s -> s.getDivision() != null && s.getDivision().toLowerCase().contains(divLower))
                    .collect(java.util.stream.Collectors.toList());
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Submissions");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Header row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Data rows
            int rowNum = 1;
            for (Submission s : submissions) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                List<String> panels = getPanelNumbers(s.getId());
                row.createCell(0).setCellValue(s.getId());
                row.createCell(1).setCellValue(nullSafe(s.getServiceNumber()));
                row.createCell(2).setCellValue(nullSafe(s.getCustomerName()));
                row.createCell(3).setCellValue(nullSafe(s.getPhone()));
                row.createCell(4).setCellValue(nullSafe(s.getAddress()));
                row.createCell(5).setCellValue(nullSafe(s.getDivision()));
                row.createCell(6).setCellValue(nullSafe(s.getSubDivision()));
                row.createCell(7).setCellValue(nullSafe(s.getSection()));
                row.createCell(8).setCellValue(nullSafe(s.getDistribution()));
                row.createCell(9).setCellValue(nullSafe(s.getInverterSerialNumber()));
                row.createCell(10).setCellValue(panels.size() > 0 ? panels.get(0) : "");
                row.createCell(11).setCellValue(panels.size() > 1 ? panels.get(1) : "");
                row.createCell(12).setCellValue(panels.size() > 2 ? panels.get(2) : "");
                row.createCell(13).setCellValue(panels.size() > 3 ? panels.get(3) : "");
                row.createCell(14).setCellValue(nullSafe(s.getSurveyorName()));
                row.createCell(15).setCellValue(s.getSurveyor() != null ? s.getSurveyor().getEmail() : "");
                row.createCell(16).setCellValue(s.getStatus().name());
                row.createCell(17).setCellValue(s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
            }

            workbook.write(out);
            log.info("Excel export generated with {} rows", submissions.size());
            return out.toByteArray();
        }
    }

    // ── PDF export ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportPdf(Long surveyorId, SubmissionStatus status,
                            String division, String serviceNumber,
                            LocalDateTime from, LocalDateTime to) throws IOException {

        List<Submission> submissions = submissionRepository
                .findAllFilteredForExport(surveyorId, status, serviceNumber, from, to);

        // Case-insensitive division filter applied in Java (avoids LOWER(bytea) PostgreSQL type error)
        if (division != null && !division.isBlank()) {
            final String divLower = division.toLowerCase();
            submissions = submissions.stream()
                    .filter(s -> s.getDivision() != null && s.getDivision().toLowerCase().contains(divLower))
                    .collect(java.util.stream.Collectors.toList());
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // A3 landscape gives ~1122 x 794 pt — much more room for 18 columns
            Document document = new Document(PageSize.A3.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont  = new Font(Font.HELVETICA, 13, Font.BOLD,  Color.decode("#1976D2"));
            Font headerFont = new Font(Font.HELVETICA,  6, Font.BOLD,  Color.WHITE);
            Font cellFont   = new Font(Font.HELVETICA,  6, Font.NORMAL, Color.BLACK);

            Paragraph title = new Paragraph("Helios Green Light Solar Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            PdfPTable table = new PdfPTable(HEADERS.length);
            table.setWidthPercentage(100);
            // Proportional column widths — wider for address/email, narrower for panels/ID
            table.setWidths(new float[]{
                    2f,   // ID
                    6f,   // Service Number
                    6f,   // Customer Name
                    5f,   // Phone
                    9f,   // Address
                    5f,   // Division
                    5f,   // Sub Division
                    4f,   // Section
                    5f,   // Distribution
                    6f,   // Inverter Serial No.
                    4f,   // Panel 1
                    4f,   // Panel 2
                    4f,   // Panel 3
                    4f,   // Panel 4
                    5f,   // Surveyor Name
                    7f,   // Surveyor Email
                    5f,   // Status
                    6f    // Created At
            });

            for (String header : HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(Color.decode("#1976D2"));
                cell.setPadding(3);
                cell.setMinimumHeight(16f);
                table.addCell(cell);
            }

            for (Submission s : submissions) {
                List<String> panels = getPanelNumbers(s.getId());
                // Format createdAt as a readable date without the nanosecond noise
                String createdAt = s.getCreatedAt() != null
                        ? s.getCreatedAt().toLocalDate().toString()
                        + " " + s.getCreatedAt().toLocalTime().withNano(0).toString()
                        : "";
                String[] values = {
                        String.valueOf(s.getId()),
                        nullSafe(s.getServiceNumber()),
                        nullSafe(s.getCustomerName()),
                        nullSafe(s.getPhone()),
                        nullSafe(s.getAddress()),
                        nullSafe(s.getDivision()),
                        nullSafe(s.getSubDivision()),
                        nullSafe(s.getSection()),
                        nullSafe(s.getDistribution()),
                        nullSafe(s.getInverterSerialNumber()),
                        panels.size() > 0 ? panels.get(0) : "",
                        panels.size() > 1 ? panels.get(1) : "",
                        panels.size() > 2 ? panels.get(2) : "",
                        panels.size() > 3 ? panels.get(3) : "",
                        nullSafe(s.getSurveyorName()),
                        s.getSurveyor() != null ? s.getSurveyor().getEmail() : "",
                        s.getStatus().name(),
                        createdAt
                };
                for (String val : values) {
                    PdfPCell cell = new PdfPCell(new Phrase(val, cellFont));
                    cell.setPadding(3);
                    cell.setMinimumHeight(14f);
                    table.addCell(cell);
                }
            }

            document.add(table);
            document.close();
            log.info("PDF export generated with {} rows", submissions.size());
            return out.toByteArray();
        }
    }

    // ── Excel import ──────────────────────────────────────────────────────────

    public int importFromExcel(MultipartFile file) throws IOException {
        List<AdminSubmissionRequest> requests = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) continue;

                AdminSubmissionRequest req = new AdminSubmissionRequest();
                req.setServiceNumber(getCellValue(row, 0));
                req.setCustomerName(getCellValue(row, 1));
                req.setPhone(getCellValue(row, 2));
                req.setAddress(getCellValue(row, 3));
                req.setDivision(getCellValue(row, 4));
                req.setSubDivision(getCellValue(row, 5));
                req.setSection(getCellValue(row, 6));
                req.setDistribution(getCellValue(row, 7));

                if (req.getServiceNumber() != null && !req.getServiceNumber().isBlank()) {
                    requests.add(req);
                }
            }
        }

        submissionService.bulkCreate(requests);
        log.info("Excel import completed — {} records processed", requests.size());
        return requests.size();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<String> getPanelNumbers(Long submissionId) {
        return panelNumberRepository
                .findBySubmissionIdOrderBySequenceAsc(submissionId)
                .stream()
                .map(PanelNumber::getPanelNumber)
                .collect(Collectors.toList());
    }

    private String getCellValue(org.apache.poi.ss.usermodel.Row row, int index) {
        org.apache.poi.ss.usermodel.Cell cell = row.getCell(index);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}