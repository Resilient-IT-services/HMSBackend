package com.res.hms.ipd.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.iam.infrastructure.security.SecurityConstants;
import com.res.hms.ipd.application.dto.AdmissionRequestDTO;
import com.res.hms.ipd.application.dto.AdmissionResponseDTO;
import com.res.hms.ipd.application.dto.BedTransferResponse;
import com.res.hms.ipd.application.dto.TransferBedRequest;
import com.res.hms.ipd.application.service.AdmissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "IPD Admissions")
@RestController
@RequestMapping("/api/v1/ipd/admissions")
@RequiredArgsConstructor
public class AdmissionController {

  private final AdmissionService admissionService;

  @PostMapping
  @PreAuthorize(SecurityConstants.HAS_ADMIN_RECEPTIONIST_OR_NURSE)
  public ApiResponse<AdmissionResponseDTO> admit(@Valid @RequestBody AdmissionRequestDTO request) {
    return ApiResponse.success(admissionService.admit(request));
  }

  @GetMapping
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<PageResponse<AdmissionResponseDTO>> list(Pageable pageable) {
    return ApiResponse.success(admissionService.list(pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<AdmissionResponseDTO> get(@PathVariable UUID id) {
    return ApiResponse.success(admissionService.get(id));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_RECEPTIONIST_OR_NURSE)
  public ApiResponse<AdmissionResponseDTO> updateStatus(
      @PathVariable UUID id, @RequestParam com.res.hms.ipd.domain.AdmissionStatus status) {
    return ApiResponse.success(admissionService.updateStatus(id, status));
  }

  @PostMapping("/{id}/transfer")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_RECEPTIONIST_OR_NURSE)
  public ApiResponse<BedTransferResponse> transfer(
      @PathVariable UUID id, @Valid @RequestBody TransferBedRequest request) {
    return ApiResponse.success(admissionService.transferBed(id, request));
  }

  @GetMapping("/{id}/transfers")
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<List<BedTransferResponse>> getTransfers(@PathVariable UUID id) {
    return ApiResponse.success(admissionService.getTransfers(id));
  }
}
