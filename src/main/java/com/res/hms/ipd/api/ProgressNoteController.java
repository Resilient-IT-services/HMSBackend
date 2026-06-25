package com.res.hms.ipd.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.iam.infrastructure.security.SecurityConstants;
import com.res.hms.ipd.application.dto.CreateProgressNoteRequest;
import com.res.hms.ipd.application.dto.ProgressNoteResponse;
import com.res.hms.ipd.application.service.ProgressNoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "IPD Progress Notes")
@RestController
@RequestMapping({"/api/ipd/admissions", "/api/v1/ipd/admissions"})
@RequiredArgsConstructor
public class ProgressNoteController {

  private final ProgressNoteService progressNoteService;

  @PostMapping("/{id}/progress-notes")
  @PreAuthorize(SecurityConstants.IPD_PROGRESS_NOTE_CREATE)
  public ApiResponse<ProgressNoteResponse> create(
      @PathVariable UUID id, @Valid @RequestBody CreateProgressNoteRequest request) {
    return ApiResponse.success(progressNoteService.create(id, request));
  }

  @GetMapping("/{id}/progress-notes")
  @PreAuthorize(SecurityConstants.IPD_PROGRESS_NOTE_VIEW)
  public ApiResponse<List<ProgressNoteResponse>> findByAdmission(@PathVariable UUID id) {
    return ApiResponse.success(progressNoteService.findByAdmission(id));
  }
}
