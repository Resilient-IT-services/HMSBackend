package com.res.hms.pharmacy.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleRequest;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleResponse;
import com.res.hms.pharmacy.application.service.QuickPharmacySaleService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pharmacy/quick-sale")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','SUPER_ADMIN')")
public class PharmacyQuickSaleController {

  private final QuickPharmacySaleService quickPharmacySaleService;

  @PostMapping
  public ApiResponse<QuickPharmacySaleResponse> createQuickSale(@RequestBody QuickPharmacySaleRequest request) {
    return ApiResponse.success(quickPharmacySaleService.createQuickSale(request));
  }

  @GetMapping("/{id}")
  public ApiResponse<QuickPharmacySaleResponse> getSale(@PathVariable UUID id) {
    return ApiResponse.success(quickPharmacySaleService.getSale(id));
  }

  @GetMapping("/prescription/{prescriptionId}")
  public ApiResponse<QuickPharmacySaleResponse> getSaleByPrescription(@PathVariable UUID prescriptionId) {
    return ApiResponse.success(quickPharmacySaleService.getSaleByPrescription(prescriptionId));
  }

  @GetMapping("/recent-customers")
  public ApiResponse<List<Object[]>> searchRecentCustomers(@RequestParam String search) {
    return ApiResponse.success(quickPharmacySaleService.searchRecentCustomers(search));
  }
}
