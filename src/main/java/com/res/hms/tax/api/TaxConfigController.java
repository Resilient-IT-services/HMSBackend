package com.res.hms.tax.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.tax.application.dto.TaxGroupDTO;
import com.res.hms.tax.application.dto.TaxMasterDTO;
import com.res.hms.tax.application.dto.TaxRuleDTO;
import com.res.hms.tax.application.service.TaxConfigurationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Taxation Configuration")
@RestController
@RequestMapping("/api/v1/tax/config")
@RequiredArgsConstructor
public class TaxConfigController {

  private final TaxConfigurationService configService;

  @GetMapping("/masters")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<List<TaxMasterDTO>> getAllMasters() {
    return ApiResponse.success(configService.getAllMasters());
  }

  @PostMapping("/masters")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<TaxMasterDTO> createMaster(@Valid @RequestBody TaxMasterDTO request) {
    return ApiResponse.success(configService.createMaster(request));
  }

  @GetMapping("/groups")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<List<TaxGroupDTO>> getAllGroups() {
    return ApiResponse.success(configService.getAllGroups());
  }

  @PostMapping("/groups")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<TaxGroupDTO> createGroup(@Valid @RequestBody TaxGroupDTO request) {
    return ApiResponse.success(configService.createGroup(request));
  }

  @GetMapping("/rules")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<List<TaxRuleDTO>> getAllRules() {
    return ApiResponse.success(configService.getAllRules());
  }

  @PostMapping("/rules")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<TaxRuleDTO> createRule(@Valid @RequestBody TaxRuleDTO request) {
    return ApiResponse.success(configService.createRule(request));
  }
}
