package com.res.hms.hospital.application.service;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.hospital.application.dto.HospitalDTO;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HospitalService {

  private final HospitalRepository hospitalRepository;

  @Transactional(readOnly = true)
  public HospitalDTO getById(UUID id) {
    return hospitalRepository
        .findById(id)
        .map(this::toDto)
        .orElseThrow(
            () -> new ApiException("NOT_FOUND", "Hospital not found", HttpStatus.NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public PageResponse<HospitalDTO> list(
      org.springframework.data.domain.Pageable pageable,
      String search,
      java.util.Map<String, String> filters) {
    com.res.hms.core.util.SpecificationBuilder<Hospital> builder =
        new com.res.hms.core.util.SpecificationBuilder<>(
            filters, search, java.util.List.of("name", "code"));

    return PageResponse.from(
        hospitalRepository.findAll(builder.build(), pageable).map(this::toDto));
  }

  @Transactional(readOnly = true)
  public java.util.List<com.res.hms.hospital.application.dto.HospitalSelectionDTO>
      getHospitalSelection() {
    return hospitalRepository.findAll().stream()
        .filter(h -> !h.isDeleted())
        .map(
            h ->
                com.res.hms.hospital.application.dto.HospitalSelectionDTO.builder()
                    .id(h.getId())
                    .name(h.getName())
                    .build())
        .collect(Collectors.toList());
  }

  public HospitalDTO toDto(Hospital hospital) {
    return HospitalDTO.builder()
        .id(hospital.getId())
        .name(hospital.getName())
        .code(hospital.getCode())
        .address(hospital.getAddress())
        .city(hospital.getCity())
        .state(hospital.getState())
        .pincode(hospital.getPincode())
        .status(hospital.getStatus())
        .gstin(hospital.getGstin())
        .registrationNo(hospital.getRegistrationNo())
        .timezone(hospital.getTimezone())
        .settings(hospital.getSettings())
        .departments(
            hospital.getDepartments().stream()
                .map(com.res.hms.hospital.domain.Department::getName)
                .collect(Collectors.toList()))
        .build();
  }
}
