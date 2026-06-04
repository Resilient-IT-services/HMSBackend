package com.res.hms.lab.application.service;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.lab.application.dto.LabPriceDTO;
import com.res.hms.lab.domain.LabPrice;
import com.res.hms.lab.infrastructure.repository.LabPriceRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabPriceService {

  private final LabPriceRepository repository;
  private final LabMapper mapper;
  private final HospitalContext hospitalContext;

  @Transactional(readOnly = true)
  public List<LabPriceDTO> listAll() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return repository.findAll().stream()
        .filter(p -> p.getHospitalId().equals(hospitalId))
        .map(mapper::toDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public LabPriceDTO getByTest(UUID testId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return repository.findAll().stream()
        .filter(p -> p.getHospitalId().equals(hospitalId))
        .filter(p -> p.getTestId() != null && p.getTestId().equals(testId))
        .findFirst()
        .map(mapper::toDto)
        .orElse(null);
  }

  @Transactional
  public LabPriceDTO savePrice(LabPriceDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    LabPrice entity;
    if (dto.getId() != null) {
      entity =
          repository
              .findById(dto.getId())
              .orElseThrow(() -> new RuntimeException("Price record not found"));
    } else {
      entity = new LabPrice();
      entity.setHospitalId(hospitalId);
    }

    entity.setTestId(dto.getTestId());
    entity.setPackageId(dto.getPackageId());
    entity.setStandardPrice(dto.getStandardPrice());
    entity.setEmergencyPrice(dto.getEmergencyPrice());
    entity.setInsurancePrice(dto.getInsurancePrice());
    entity.setEffectiveFrom(dto.getEffectiveFrom());

    return mapper.toDto(repository.save(entity));
  }
}
