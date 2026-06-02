package com.res.hms.tax.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.iam.infrastructure.security.SecurityConstants;
import com.res.hms.tax.application.dto.TaxGroupDTO;
import com.res.hms.tax.application.dto.TaxMasterDTO;
import com.res.hms.tax.application.dto.TaxRuleDTO;
import com.res.hms.tax.application.service.TaxConfigurationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Taxation Configuration")
@RestController
@RequestMapping("/api/v1/tax/config")
@RequiredArgsConstructor
public class TaxConfigController {

    private final TaxConfigurationService configService;

    @GetMapping("/masters")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TaxMasterDTO>> getAllMasters() {
        return ApiResponse.success(configService.getAllMasters());
    }

    @PostMapping("/masters")
    @PreAuthorize(SecurityConstants.HAS_ADMIN)
    public ApiResponse<TaxMasterDTO> createMaster(@Valid @RequestBody TaxMasterDTO request) {
        return ApiResponse.success(configService.createMaster(request));
    }

    @PutMapping("/masters/{id}")
    @PreAuthorize(SecurityConstants.HAS_ADMIN)
    public ApiResponse<TaxMasterDTO> updateMaster(@PathVariable UUID id, @Valid @RequestBody TaxMasterDTO request) {
        return ApiResponse.success(configService.updateMaster(id, request));
    }

    @DeleteMapping("/masters/{id}")
    @PreAuthorize(SecurityConstants.HAS_ADMIN)
    public ApiResponse<Void> deleteMaster(@PathVariable UUID id) {
        configService.deleteMaster(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/groups")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TaxGroupDTO>> getAllGroups() {
        return ApiResponse.success(configService.getAllGroups());
    }

    @PostMapping("/groups")
    @PreAuthorize(SecurityConstants.HAS_ADMIN)
    public ApiResponse<TaxGroupDTO> createGroup(@Valid @RequestBody TaxGroupDTO request) {
        return ApiResponse.success(configService.createGroup(request));
    }

    @PutMapping("/groups/{id}")
    @PreAuthorize(SecurityConstants.HAS_ADMIN)
    public ApiResponse<TaxGroupDTO> updateGroup(@PathVariable UUID id, @Valid @RequestBody TaxGroupDTO request) {
        return ApiResponse.success(configService.updateGroup(id, request));
    }

    @DeleteMapping("/groups/{id}")
    @PreAuthorize(SecurityConstants.HAS_ADMIN)
    public ApiResponse<Void> deleteGroup(@PathVariable UUID id) {
        configService.deleteGroup(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/rules")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TaxRuleDTO>> getAllRules() {
        return ApiResponse.success(configService.getAllRules());
    }

    @PostMapping("/rules")
    @PreAuthorize(SecurityConstants.HAS_ADMIN)
    public ApiResponse<TaxRuleDTO> createRule(@Valid @RequestBody TaxRuleDTO request) {
        return ApiResponse.success(configService.createRule(request));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize(SecurityConstants.HAS_ADMIN)
    public ApiResponse<TaxRuleDTO> updateRule(@PathVariable UUID id, @Valid @RequestBody TaxRuleDTO request) {
        return ApiResponse.success(configService.updateRule(id, request));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize(SecurityConstants.HAS_ADMIN)
    public ApiResponse<Void> deleteRule(@PathVariable UUID id) {
        configService.deleteRule(id);
        return ApiResponse.success(null);
    }
}
