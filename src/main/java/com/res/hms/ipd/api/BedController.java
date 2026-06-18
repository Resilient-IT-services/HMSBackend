package com.res.hms.ipd.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.iam.infrastructure.security.SecurityConstants;
import com.res.hms.ipd.application.dto.BedDTO;
import com.res.hms.ipd.application.service.BedService;
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

@Tag(name = "IPD Beds")
@RestController
@RequestMapping("/api/v1/ipd/beds")
@RequiredArgsConstructor
public class BedController {

  private final BedService bedService;

  @PostMapping
  @PreAuthorize(SecurityConstants.HAS_ADMIN)
  public ApiResponse<BedDTO> create(@Valid @RequestBody BedDTO request) {
    return ApiResponse.success(bedService.create(request));
  }

  @GetMapping
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<PageResponse<BedDTO>> list(Pageable pageable) {
    return ApiResponse.success(bedService.list(pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<BedDTO> get(@PathVariable UUID id) {
    return ApiResponse.success(bedService.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize(SecurityConstants.HAS_ADMIN)
  public ApiResponse<BedDTO> update(@PathVariable UUID id, @Valid @RequestBody BedDTO request) {
    return ApiResponse.success(bedService.update(id, request));
  }
}
