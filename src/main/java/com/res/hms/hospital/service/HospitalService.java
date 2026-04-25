package com.res.hms.hospital.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.common.api.PageResponse;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.HospitalStatus;
import com.res.hms.hospital.dto.CreateHospitalRequest;
import com.res.hms.hospital.dto.HospitalResponse;
import com.res.hms.repository.HospitalRepository;
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

  @Transactional
  public HospitalResponse create(CreateHospitalRequest request) {
    if (hospitalRepository.findByCode(request.code()).isPresent()) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Hospital code already exists", HttpStatus.CONFLICT);
    }

    Hospital hospital = new Hospital();
    hospital.setName(request.name());
    hospital.setCode(request.code());
    hospital.setAddress(request.address());
    hospital.setStatus(HospitalStatus.ACTIVE);
    hospitalRepository.save(hospital);
    return toResponse(hospital);
  }

  @Transactional
  public HospitalResponse update(UUID id, CreateHospitalRequest request) {
    Hospital hospital =
        hospitalRepository
            .findById(id)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Hospital not found", HttpStatus.NOT_FOUND));

    // Check if code is being changed and if it conflicts
    if (!hospital.getCode().equals(request.code())
        && hospitalRepository.findByCode(request.code()).isPresent()) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Hospital code already exists", HttpStatus.CONFLICT);
    }

    hospital.setName(request.name());
    hospital.setCode(request.code());
    hospital.setAddress(request.address());
    hospitalRepository.save(hospital);
    return toResponse(hospital);
  }

  @Transactional(readOnly = true)
  public PageResponse<HospitalResponse> list(
      int page, int size, String name, HospitalStatus status) {
    Specification<Hospital> spec = Specification.where(null);
    if (name != null && !name.isBlank()) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
    }
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }

    return PageResponse.from(
        hospitalRepository.findAll(spec, PageRequest.of(page, size)).map(this::toResponse));
  }

  private HospitalResponse toResponse(Hospital hospital) {
    return new HospitalResponse(
        hospital.getId(),
        hospital.getName(),
        hospital.getCode(),
        hospital.getAddress(),
        hospital.getStatus(),
        hospital.getSettings(),
        hospital.getCreatedAt());
  }
}
