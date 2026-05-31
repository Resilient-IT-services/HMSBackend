package com.res.hms.pharmacy.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.iam.infrastructure.security.SecurityConstants;
import com.res.hms.pharmacy.application.dto.MedicineDTO;
import com.res.hms.pharmacy.application.dto.MedicineImportResponse;
import com.res.hms.pharmacy.application.dto.MedicineSummaryDTO;
import com.res.hms.pharmacy.application.dto.PharmacyReturnRequest;
import com.res.hms.pharmacy.application.dto.PharmacyReturnResponse;
import com.res.hms.pharmacy.application.dto.PharmacyStockDTO;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleRequest;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleResponse;
import com.res.hms.pharmacy.application.dto.StockImportResponse;
import com.res.hms.pharmacy.application.dto.StockRequest;
import com.res.hms.pharmacy.application.service.MedicineService;
import com.res.hms.pharmacy.application.service.PharmacySaleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Pharmacy")
@RestController
@RequestMapping("/api/v1/pharmacy")
@RequiredArgsConstructor
public class MedicineController {

  private final MedicineService medicineService;
  private final PharmacySaleService pharmacySaleService;

  @PostMapping("/medicines/import")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<MedicineImportResponse> importMedicines(@RequestParam MultipartFile file) {
    return ApiResponse.success(medicineService.importExcel(file));
  }

  @PostMapping("/medicines")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<MedicineDTO> create(@Valid @RequestBody MedicineDTO request) {
    return ApiResponse.success(medicineService.create(request));
  }

  @GetMapping("/medicines")
  @PreAuthorize(SecurityConstants.HAS_PHARMACY_ACCESS)
  public ApiResponse<PageResponse<MedicineDTO>> list(
      @org.springframework.data.web.PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
          org.springframework.data.domain.Pageable pageable,
      @RequestParam(required = false) String search,
      @RequestParam java.util.Map<String, String> allParams) {
    return ApiResponse.success(medicineService.list(pageable, search, allParams));
  }

  @GetMapping("/medicines/summary")
  @PreAuthorize(SecurityConstants.HAS_PHARMACY_ACCESS)
  public ApiResponse<PageResponse<MedicineSummaryDTO>> listSummary(
      @org.springframework.data.web.PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
          org.springframework.data.domain.Pageable pageable,
      @RequestParam(required = false) String search,
      @RequestParam java.util.Map<String, String> allParams) {
    return ApiResponse.success(medicineService.listSummary(pageable, search, allParams));
  }

  @GetMapping("/medicines/{id}")
  @PreAuthorize(SecurityConstants.HAS_PHARMACY_ACCESS)
  public ApiResponse<MedicineDTO> get(@PathVariable UUID id) {
    return ApiResponse.success(medicineService.get(id));
  }

  @PutMapping("/medicines/{id}")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_PHARMACIST)
  public ApiResponse<MedicineDTO> update(
      @PathVariable UUID id, @Valid @RequestBody MedicineDTO request) {
    return ApiResponse.success(medicineService.update(id, request));
  }

  @DeleteMapping("/medicines/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    medicineService.delete(id);
    return ApiResponse.success(null);
  }

  @PostMapping("/stock/import")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_PHARMACIST)
  public ApiResponse<StockImportResponse> importStock(@RequestParam MultipartFile file) {
    return ApiResponse.success(medicineService.importStockExcel(file));
  }

  @PostMapping("/stock")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_PHARMACIST)
  public ApiResponse<PharmacyStockDTO> addStock(@Valid @RequestBody StockRequest request) {
    return ApiResponse.success(medicineService.addStock(request));
  }

  @GetMapping("/stock/export")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_PHARMACIST)
  public void exportStock(HttpServletResponse response) throws IOException {
    medicineService.exportStockExcel(response);
  }

  @GetMapping("/stock")
  @PreAuthorize(SecurityConstants.HAS_PHARMACY_ACCESS)
  public ApiResponse<PageResponse<PharmacyStockDTO>> listStock(
      @org.springframework.data.web.PageableDefault(sort = "medicine.name", direction = org.springframework.data.domain.Sort.Direction.ASC)
          org.springframework.data.domain.Pageable pageable,
      @RequestParam(required = false) String search,
      @RequestParam java.util.Map<String, String> allParams) {
    return ApiResponse.success(medicineService.listStock(pageable, search, allParams));
  }

  @PostMapping("/sales")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_PHARMACIST)
  public ApiResponse<QuickPharmacySaleResponse> createSale(
      @Valid @RequestBody QuickPharmacySaleRequest request) {
    return ApiResponse.success(pharmacySaleService.createSale(request));
  }

  @PostMapping("/prescriptions/{id}/dispense")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_PHARMACIST)
  public ApiResponse<QuickPharmacySaleResponse> dispense(
      @PathVariable UUID id, @Valid @RequestBody QuickPharmacySaleRequest request) {
    request.setPrescriptionId(id);
    return ApiResponse.success(pharmacySaleService.createSale(request));
  }

  @GetMapping("/sales/{id}")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_PHARMACIST)
  public ApiResponse<QuickPharmacySaleResponse> getSale(@PathVariable UUID id) {
    return ApiResponse.success(pharmacySaleService.getSale(id));
  }

  @GetMapping("/sales/prescription/{prescriptionId}")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_PHARMACIST)
  public ApiResponse<QuickPharmacySaleResponse> getSaleByPrescription(
      @PathVariable UUID prescriptionId) {
    return ApiResponse.success(pharmacySaleService.getSaleByPrescription(prescriptionId));
  }

  @GetMapping("/recent-customers")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_PHARMACIST)
  public ApiResponse<List<Object[]>> searchRecentCustomers(@RequestParam String search) {
    return ApiResponse.success(pharmacySaleService.searchRecentCustomers(search));
  }

  @PostMapping("/returns")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_PHARMACIST)
  public ApiResponse<PharmacyReturnResponse> returnStock(
      @Valid @RequestBody PharmacyReturnRequest request) {
    return ApiResponse.success(pharmacySaleService.returnStock(request));
  }

  @GetMapping("/alerts/expiry")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_PHARMACIST)
  public ApiResponse<List<PharmacyStockDTO>> expiryAlerts(
      @RequestParam(defaultValue = "30") int days) {
    return ApiResponse.success(pharmacySaleService.expiryAlerts(days));
  }

  @GetMapping("/alerts/low-stock")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_PHARMACIST)
  public ApiResponse<PageResponse<PharmacyStockDTO>> lowStockAlerts(
      @org.springframework.data.web.PageableDefault(sort = "medicine.name", direction = org.springframework.data.domain.Sort.Direction.ASC)
          org.springframework.data.domain.Pageable pageable,
      @RequestParam(required = false) String search,
      @RequestParam java.util.Map<String, String> allParams) {
    return ApiResponse.success(pharmacySaleService.lowStockAlerts(pageable, search, allParams));
  }
}
