package com.res.hms.hospital.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.HospitalStatus;
import com.res.hms.hospital.dto.HospitalDTO;
import com.res.hms.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.hospital.mapper.HospitalMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalAggregateService {

  private final HospitalRepository hospitalRepository;
  private final HospitalService hospitalService;
  private final HospitalMapper hospitalMapper;

  @Transactional
  public HospitalDTO saveHospitalAggregate(HospitalDTO dto) {
    log.info("Starting aggregate save for hospital: {}", dto.getName());

    Hospital hospital;
    if (dto.getId() == null) {
      hospital = createHospital(dto);
    } else {
      hospital =
          hospitalRepository
              .findById(dto.getId())
              .orElseThrow(
                  () -> new ApiException("NOT_FOUND", "Hospital not found", HttpStatus.NOT_FOUND));
      updateHospital(hospital, dto);
    }

    hospitalRepository.saveAndFlush(hospital);
    return hospitalService.toDto(hospital);
  }

  private Hospital createHospital(HospitalDTO dto) {
    if (hospitalRepository.existsByCode(dto.getCode())) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Hospital code already exists", HttpStatus.CONFLICT);
    }
    if (dto.getGstin() != null && hospitalRepository.existsByGstin(dto.getGstin())) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "GSTIN already exists", HttpStatus.CONFLICT);
    }

    Hospital hospital = new Hospital();
    hospital.setCode(dto.getCode());
    hospital.setStatus(dto.getStatus() != null ? dto.getStatus() : HospitalStatus.ACTIVE);
    hospitalMapper.updateEntity(dto, hospital);
    return hospital;
  }

  private void updateHospital(Hospital hospital, HospitalDTO dto) {
    if (!hospital.getCode().equals(dto.getCode())
        && hospitalRepository.existsByCodeAndIdNot(dto.getCode(), hospital.getId())) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Hospital code already exists", HttpStatus.CONFLICT);
    }
    if (dto.getGstin() != null
        && !dto.getGstin().equals(hospital.getGstin())
        && hospitalRepository.existsByGstinAndIdNot(dto.getGstin(), hospital.getId())) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "GSTIN already exists", HttpStatus.CONFLICT);
    }

    if (dto.getStatus() != null) {
      hospital.setStatus(dto.getStatus());
    }
    hospitalMapper.updateEntity(dto, hospital);
  }
}
