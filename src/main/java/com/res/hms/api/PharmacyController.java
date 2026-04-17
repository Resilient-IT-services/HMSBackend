package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.pharmacy.dto.AddPharmacyStockRequest;
import com.res.hms.pharmacy.dto.AddPrescriptionItemRequest;
import com.res.hms.pharmacy.dto.CreateMedicineRequest;
import com.res.hms.pharmacy.dto.CreatePrescriptionRequest;
import com.res.hms.pharmacy.dto.DispensePrescriptionRequest;
import com.res.hms.pharmacy.dto.DispenseResponse;
import com.res.hms.pharmacy.dto.MedicineResponse;
import com.res.hms.pharmacy.dto.PharmacyStockResponse;
import com.res.hms.pharmacy.dto.PrescriptionResponse;
import com.res.hms.pharmacy.service.PharmacyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Pharmacy")
@RestController
@RequestMapping("/api/v1/pharmacy")
@RequiredArgsConstructor
public class PharmacyController {

    private final PharmacyService pharmacyService;

    @PostMapping("/medicines")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ApiResponse<MedicineResponse> createMedicine(@Valid @RequestBody CreateMedicineRequest request) {
        return ApiResponse.success(pharmacyService.createMedicine(request));
    }

    @GetMapping("/medicines")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','DOCTOR')")
    public ApiResponse<PageResponse<MedicineResponse>> listMedicines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(pharmacyService.listMedicines(page, Math.min(size, 100)));
    }

    @PostMapping("/stock")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ApiResponse<PharmacyStockResponse> addStock(@Valid @RequestBody AddPharmacyStockRequest request) {
        return ApiResponse.success(pharmacyService.addStock(request));
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ApiResponse<List<PharmacyStockResponse>> listStock() {
        return ApiResponse.success(pharmacyService.listStock());
    }

    @PostMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public ApiResponse<PrescriptionResponse> createPrescription(@Valid @RequestBody CreatePrescriptionRequest request) {
        return ApiResponse.success(pharmacyService.createPrescription(request));
    }

    @PostMapping("/prescriptions/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public ApiResponse<PrescriptionResponse> addPrescriptionItem(@PathVariable Long id, @Valid @RequestBody AddPrescriptionItemRequest request) {
        return ApiResponse.success(pharmacyService.addPrescriptionItem(id, request));
    }

    @PostMapping("/dispense")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ApiResponse<DispenseResponse> dispense(@Valid @RequestBody DispensePrescriptionRequest request) {
        return ApiResponse.success(pharmacyService.dispense(request));
    }
}
