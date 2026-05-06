package com.res.hms.report.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.report.dto.ReportType;
import com.res.hms.report.service.ExportService;
import com.res.hms.report.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;
  private final ExportService exportService;

  @GetMapping("/preview")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<List<?>> getReportPreview(
      @RequestParam ReportType type,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) java.util.UUID doctorId) {
    return ApiResponse.success(reportService.getReportData(type, startDate, endDate, doctorId));
  }

  @GetMapping("/export/excel")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Resource> exportExcel(
      @RequestParam ReportType type,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) java.util.UUID doctorId)
      throws IOException {
    List<?> data = reportService.getReportData(type, startDate, endDate, doctorId);
    byte[] content = exportService.generateExcel(type, data, startDate, endDate);

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=" + type.name().toLowerCase() + "_report.xlsx")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(new ByteArrayResource(content));
  }

  @GetMapping("/export/pdf")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Resource> exportPdf(
      @RequestParam ReportType type,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) java.util.UUID doctorId) {
    List<?> data = reportService.getReportData(type, startDate, endDate, doctorId);
    byte[] content = exportService.generatePdf(type, data, startDate, endDate);

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=" + type.name().toLowerCase() + "_report.pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(new ByteArrayResource(content));
  }
}
