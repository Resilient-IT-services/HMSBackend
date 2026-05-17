package com.res.hms.finance.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.finance.application.dto.InvoiceDTO;
import com.res.hms.finance.application.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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
}
