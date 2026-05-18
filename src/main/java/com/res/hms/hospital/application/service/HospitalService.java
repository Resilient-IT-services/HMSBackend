package com.res.hms.hospital.application.service;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.hospital.application.dto.HospitalDTO;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.domain.HospitalStatus;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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
  public PageResponse<HospitalDTO> list(int page, int size, String name, String status) {
    Specification<Hospital> spec = Specification.where(null);
      if (name != null && !name.isBlank()) {
          spec =
              spec.and(
                  (root, query, cb) ->
                      cb.or(
                          cb.like(
                              cb.lower(root.get("name")),
                              "%" + name.toLowerCase() + "%"),
                          cb.like(
                              cb.lower(root.get("code")),
                              "%" + name.toLowerCase() + "%")
                      ));
      }
    if (status != null && !status.isBlank()) {
      try {
        HospitalStatus hospitalStatus = HospitalStatus.valueOf(status.toUpperCase());
        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), hospitalStatus));
      } catch (IllegalArgumentException e) {
        throw new ApiException(
            "VALIDATION_ERROR", "Invalid hospital status: " + status, HttpStatus.BAD_REQUEST);
      }
    }

    return PageResponse.from(
        hospitalRepository.findAll(spec, PageRequest.of(page, size)).map(this::toDto));
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
        .settings(hospital.getSettings())
        .build();
  }
}
