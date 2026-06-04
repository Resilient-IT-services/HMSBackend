package com.res.hms.finance.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.finance.application.dto.InvoiceDTO;
import com.res.hms.finance.application.service.InvoiceService;
import com.res.hms.finance.domain.InvoiceItemCategory;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance/invoices")
@RequiredArgsConstructor
public class InvoiceController {

  private final InvoiceService invoiceService;

  @GetMapping("/visit/{visitId}")
  public ApiResponse<InvoiceDTO> getByVisit(@PathVariable UUID visitId) {
    return ApiResponse.success(invoiceService.getByVisit(visitId));
  }

  @GetMapping("/{id}")
  public ApiResponse<InvoiceDTO> getById(@PathVariable UUID id) {
    return ApiResponse.success(invoiceService.getById(id));
  }

  @GetMapping("/{id}/export")
  public ResponseEntity<byte[]> exportInvoice(
      @PathVariable UUID id, @RequestParam(required = false) InvoiceItemCategory category) {

    byte[] pdf = invoiceService.exportInvoicePdf(id, category);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice.pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }
}
