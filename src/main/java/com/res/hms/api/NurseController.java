package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.nurse.dto.CreateNurseRequest;
import com.res.hms.nurse.dto.NurseResponse;
import com.res.hms.nurse.service.NurseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Nurses")
@RestController
@RequestMapping("/api/v1/nurses")
@RequiredArgsConstructor
public class NurseController {

    private final NurseService nurseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<NurseResponse> create(@Valid @RequestBody CreateNurseRequest request) {
        return ApiResponse.success(nurseService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','NURSE','RECEPTIONIST')")
    public ApiResponse<PageResponse<NurseResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(nurseService.list(page, Math.min(size, 100)));
    }
}
