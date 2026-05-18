package com.res.hms.hospital.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.core.service.DomainEventPublisher;
import com.res.hms.hospital.application.dto.HospitalDTO;
import com.res.hms.hospital.domain.Department;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.domain.HospitalStatus;
import com.res.hms.hospital.domain.event.HospitalStatusChangedEvent;
import com.res.hms.hospital.infrastructure.repository.DepartmentRepository;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalAggregateService {

  private final HospitalRepository hospitalRepository;
  private final HospitalService hospitalService;
  private final DomainEventPublisher domainEventPublisher;
  private final DepartmentRepository departmentRepository;

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

      if (hospitalRepository.existsByNameIgnoreCase(dto.getName())) {
          throw new ApiException(
              "BUSINESS_RULE_VIOLATION", "Hospital name already exists", HttpStatus.CONFLICT);
      }
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
    updateHospitalFields(hospital, dto);
    return hospital;
  }

  private void updateHospital(Hospital hospital, HospitalDTO dto) {
      if (!hospital.getName().equalsIgnoreCase(dto.getName())
          && hospitalRepository.existsByNameIgnoreCaseAndIdNot(
          dto.getName(), hospital.getId())) {

          throw new ApiException(
              "BUSINESS_RULE_VIOLATION",
              "Hospital name already exists",
              HttpStatus.CONFLICT);
      }
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

    if (dto.getStatus() != null && dto.getStatus() != hospital.getStatus()) {
      HospitalStatus oldStatus = hospital.getStatus();
      hospital.setStatus(dto.getStatus());
      domainEventPublisher.publish(
          "Hospital", hospital.getId(), new HospitalStatusChangedEvent(hospital.getId(), oldStatus, dto.getStatus()));
    }
    updateHospitalFields(hospital, dto);
  }

  private void updateHospitalFields(Hospital hospital, HospitalDTO dto) {
    hospital.setName(dto.getName());
    hospital.setAddress(dto.getAddress());
    hospital.setCity(dto.getCity());
    hospital.setState(dto.getState());
    hospital.setPincode(dto.getPincode());
    hospital.setGstin(dto.getGstin());
    hospital.setRegistrationNo(dto.getRegistrationNo());
    if (dto.getSettings() != null) {
      hospital.setSettings(dto.getSettings());
    }
    if (dto.getDepartments() != null) {
      hospital.setDepartments(dto.getDepartments().stream()
          .map(name -> departmentRepository.findByName(name)
              .orElseThrow(() -> new ApiException("NOT_FOUND", "Department " + name + " not found in catalog", HttpStatus.BAD_REQUEST)))
          .collect(Collectors.toSet()));
    } else {
      hospital.getDepartments().clear();
    }
  }
}
