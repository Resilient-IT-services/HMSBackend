package com.res.hms.lab.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.lab.application.dto.LabPriceDTO;
import com.res.hms.lab.application.service.LabPriceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Lab Pricing Management")
@RestController
@RequestMapping("/api/v1/lab/pricing")
@RequiredArgsConstructor
public class LabPriceController {

    private final LabPriceService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<LabPriceDTO>> listAll() {
        return ApiResponse.success(service.listAll());
    }

    @GetMapping("/test/{testId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<LabPriceDTO> getByTest(@PathVariable UUID testId) {
        return ApiResponse.success(service.getByTest(testId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<LabPriceDTO> savePrice(@RequestBody LabPriceDTO dto) {
        return ApiResponse.success(service.savePrice(dto));
    }
}
