package com.res.hms.iam.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.iam.domain.Specialization;
import com.res.hms.iam.infrastructure.repository.SpecializationRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/specializations")
@RequiredArgsConstructor
public class SpecializationController {

  private final SpecializationRepository specializationRepository;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<String>> listAll() {
    return ApiResponse.success(
        specializationRepository.findAll().stream()
            .filter(Specialization::isActive)
            .map(Specialization::getName)
            .collect(Collectors.toList()));
  }
}
