package com.res.hms.hospital.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.exception.ApiException;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.HospitalStatus;
import com.res.hms.hospital.dto.HospitalDTO;
import com.res.hms.hospital.repository.HospitalRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.hospital.mapper.HospitalMapper;

@Service
@RequiredArgsConstructor
public class HospitalService {

  private final HospitalRepository hospitalRepository;
  private final HospitalMapper hospitalMapper;

  @Transactional(readOnly = true)
  public HospitalDTO getById(UUID id) {
    return hospitalRepository
        .findById(id)
        .map(hospitalMapper::toDto)
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
                  cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
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
        hospitalRepository.findAll(spec, PageRequest.of(page, size)).map(hospitalMapper::toDto));
  }

  public HospitalDTO toDto(Hospital hospital) {
    return hospitalMapper.toDto(hospital);
  }
}
