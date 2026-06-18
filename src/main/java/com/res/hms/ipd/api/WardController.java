package com.res.hms.ipd.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.iam.infrastructure.security.SecurityConstants;
import com.res.hms.ipd.application.dto.WardDTO;
import com.res.hms.ipd.application.service.BedService;
import com.res.hms.ipd.application.service.WardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "IPD Wards")
@RestController
@RequestMapping("/api/v1/ipd/wards")
@RequiredArgsConstructor
public class WardController {

  private final WardService wardService;
  private final BedService bedService;

  @PostMapping
  @PreAuthorize(SecurityConstants.HAS_ADMIN)
  public ApiResponse<WardDTO> create(@Valid @RequestBody WardDTO request) {
    return ApiResponse.success(wardService.create(request));
  }

  @GetMapping
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<PageResponse<WardDTO>> list(Pageable pageable) {
    return ApiResponse.success(wardService.list(pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<WardDTO> get(@PathVariable UUID id) {
    return ApiResponse.success(wardService.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize(SecurityConstants.HAS_ADMIN)
  public ApiResponse<WardDTO> update(@PathVariable UUID id, @Valid @RequestBody WardDTO request) {
    return ApiResponse.success(wardService.update(id, request));
  }

  @GetMapping("/{wardId}/beds")
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<java.util.List<com.res.hms.ipd.application.dto.BedDTO>> listBeds(
      @PathVariable UUID wardId) {
    return ApiResponse.success(bedService.listByWard(wardId));
  }
}
