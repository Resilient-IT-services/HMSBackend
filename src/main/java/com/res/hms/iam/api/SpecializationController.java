package com.res.hms.iam.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.iam.domain.Specialization;
import com.res.hms.iam.infrastructure.repository.SpecializationRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

  @PostMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Specialization> create(@RequestBody Specialization specialization) {
    if (specializationRepository.findByName(specialization.getName()).isPresent()) {
        throw new com.res.hms.core.exception.ApiException("ALREADY_EXISTS", "Specialization already exists", HttpStatus.CONFLICT);
    }
    return ApiResponse.success(specializationRepository.save(specialization));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Specialization> update(@PathVariable UUID id, @RequestBody Specialization specialization) {
    Specialization existing = specializationRepository.findById(id)
        .orElseThrow(() -> new com.res.hms.core.exception.ApiException("NOT_FOUND", "Specialization not found", HttpStatus.NOT_FOUND));
    existing.setName(specialization.getName());
    existing.setDescription(specialization.getDescription());
    existing.setActive(specialization.isActive());
    return ApiResponse.success(specializationRepository.save(existing));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    specializationRepository.deleteById(id);
    return ApiResponse.success(null);
  }
}
