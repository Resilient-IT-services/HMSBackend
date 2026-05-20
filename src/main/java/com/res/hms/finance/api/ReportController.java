package com.res.hms.finance.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.finance.application.service.ExportService;
import com.res.hms.finance.application.service.ReportService;
import com.res.hms.finance.application.service.ReportType;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
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
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant to,
      @RequestParam(required = false) java.util.UUID doctorId,
      @RequestParam(defaultValue = "false") boolean allHospitals) {
    return ApiResponse.success(reportService.getReportData(type, from, to, doctorId, allHospitals));
  }

  @GetMapping("/export/excel")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Resource> exportExcel(
      @RequestParam ReportType type,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant to,
      @RequestParam(required = false) java.util.UUID doctorId,
      @RequestParam(defaultValue = "false") boolean allHospitals)
      throws IOException {
    List<?> data = reportService.getReportData(type, from, to, doctorId, allHospitals);
    byte[] content = exportService.generateExcel(type, data, from, to);

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
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant to,
      @RequestParam(required = false) java.util.UUID doctorId,
      @RequestParam(defaultValue = "false") boolean allHospitals) {
    List<?> data = reportService.getReportData(type, from, to, doctorId, allHospitals);
    byte[] content = exportService.generatePdf(type, data, from, to);

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=" + type.name().toLowerCase() + "_report.pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(new ByteArrayResource(content));
  }
}
