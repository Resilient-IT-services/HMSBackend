package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.pharmacy.dto.MedicineDTO;
import com.res.hms.pharmacy.dto.MedicineImportResponse;
import com.res.hms.pharmacy.dto.PharmacyReturnRequest;
import com.res.hms.pharmacy.dto.PharmacyReturnResponse;
import com.res.hms.pharmacy.dto.PharmacySaleRequest;
import com.res.hms.pharmacy.dto.PharmacySaleResponse;
import com.res.hms.pharmacy.dto.PharmacyStockDTO;
import com.res.hms.pharmacy.dto.StockRequest;
import com.res.hms.pharmacy.service.MedicineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','DOCTOR','NURSE','SUPER_ADMIN')")
  public ApiResponse<PageResponse<MedicineDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search) {
    return ApiResponse.success(medicineService.list(page, Math.min(size, 100), search));
  }

  @GetMapping("/medicines/available")
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','DOCTOR','NURSE','SUPER_ADMIN')")
  public ApiResponse<PageResponse<MedicineDTO>> searchAvailable(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false, defaultValue = "") String search) {
    return ApiResponse.success(medicineService.searchAvailable(page, Math.min(size, 100), search));
  }

  @GetMapping("/medicines/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','DOCTOR','NURSE','SUPER_ADMIN')")
  public ApiResponse<MedicineDTO> get(@PathVariable UUID id) {
    return ApiResponse.success(medicineService.get(id));
  }

  @PutMapping("/medicines/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
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

  @PostMapping("/stock")
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','SUPER_ADMIN')")
  public ApiResponse<PharmacyStockDTO> addStock(@Valid @RequestBody StockRequest request) {
    return ApiResponse.success(medicineService.addStock(request));
  }

  @GetMapping("/stock")
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','DOCTOR','NURSE','SUPER_ADMIN')")
  public ApiResponse<PageResponse<PharmacyStockDTO>> listStock(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "false") boolean lowStock) {
    return ApiResponse.success(medicineService.listStock(page, Math.min(size, 100), lowStock));
  }

  @PostMapping("/sales")
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','SUPER_ADMIN')")
  public ApiResponse<PharmacySaleResponse> createSale(
      @Valid @RequestBody PharmacySaleRequest request) {
    return ApiResponse.success(medicineService.createSale(request));
  }

  @PostMapping("/returns")
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','SUPER_ADMIN')")
  public ApiResponse<PharmacyReturnResponse> returnStock(
      @Valid @RequestBody PharmacyReturnRequest request) {
    return ApiResponse.success(medicineService.returnStock(request));
  }

  @GetMapping("/alerts/expiry")
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','SUPER_ADMIN')")
  public ApiResponse<List<PharmacyStockDTO>> expiryAlerts(
      @RequestParam(defaultValue = "30") int days) {
    return ApiResponse.success(medicineService.expiryAlerts(days));
  }

  @GetMapping("/alerts/low-stock")
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','SUPER_ADMIN')")
  public ApiResponse<PageResponse<PharmacyStockDTO>> lowStockAlerts(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "20") int threshold) {
    return ApiResponse.success(
        medicineService.lowStockAlerts(page, Math.min(size, 100), threshold));
  }
}
