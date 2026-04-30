package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.pharmacy.dto.MedicineDTO;
import com.res.hms.pharmacy.dto.PharmacyStockDTO;
import com.res.hms.pharmacy.service.MedicineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Tag(name = "Pharmacy")
@RestController
@RequestMapping("/api/v1/pharmacy/medicines")
@RequiredArgsConstructor
public class MedicineController {

  @GetMapping("/stock")
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','DOCTOR','NURSE','SUPER_ADMIN')")
  public ApiResponse<PageResponse<PharmacyStockDTO>> listStock(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "false") boolean lowStock) {
    return ApiResponse.success(medicineService.listStock(page, size, lowStock));
  }

  private final MedicineService medicineService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','SUPER_ADMIN')")
  public ApiResponse<MedicineDTO> create(@Valid @RequestBody MedicineDTO request) {
    return ApiResponse.success(medicineService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','DOCTOR','NURSE','SUPER_ADMIN')")
  public ApiResponse<PageResponse<MedicineDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search) {
    return ApiResponse.success(medicineService.list(page, Math.min(size, 100), search));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','DOCTOR','NURSE','SUPER_ADMIN')")
  public ApiResponse<MedicineDTO> get(@PathVariable UUID id) {
    return ApiResponse.success(medicineService.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','SUPER_ADMIN')")
  public ApiResponse<MedicineDTO> update(
      @PathVariable UUID id, @Valid @RequestBody MedicineDTO request) {
    return ApiResponse.success(medicineService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','SUPER_ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    medicineService.delete(id);
    return ApiResponse.success(null);
  }
}
