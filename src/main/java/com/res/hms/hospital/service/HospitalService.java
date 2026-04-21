package com.res.hms.hospital.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.domain.Hospital;
import com.res.hms.hospital.dto.CreateHospitalRequest;
import com.res.hms.hospital.dto.HospitalResponse;
import com.res.hms.repository.HospitalRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
    hospital.setStatus("ACTIVE");
    hospitalRepository.save(hospital);
    return toResponse(hospital);
  }

  @Transactional
  public HospitalResponse update(Long id, CreateHospitalRequest request) {
    Hospital hospital = hospitalRepository.findById(id)
        .orElseThrow(() -> new ApiException("NOT_FOUND", "Hospital not found", HttpStatus.NOT_FOUND));
    
    // Check if code is being changed and if it conflicts
    if (!hospital.getCode().equals(request.code()) && hospitalRepository.findByCode(request.code()).isPresent()) {
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
  public List<HospitalResponse> list() {
    return hospitalRepository.findAll().stream().map(this::toResponse).toList();
  }

  private HospitalResponse toResponse(Hospital hospital) {
    return new HospitalResponse(
        hospital.getId(),
        hospital.getName(),
        hospital.getCode(),
        hospital.getAddress(),
        hospital.getStatus(),
        hospital.getCreatedAt());
  }
}
