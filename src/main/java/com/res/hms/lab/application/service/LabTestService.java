package com.res.hms.lab.application.service;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.lab.application.dto.LabTestDTO;
import com.res.hms.lab.domain.LabTest;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabTestService {

  private final LabTestRepository repository;
  private final LabMapper mapper;
  private final HospitalContext hospitalContext;

  @Transactional(readOnly = true)
  public List<LabTestDTO> listAll() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return repository.findAll().stream()
        .filter(t -> t.getHospitalId().equals(hospitalId))
        .map(mapper::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public LabTestDTO create(LabTestDTO dto) {
    LabTest entity = mapper.toEntity(dto);
    entity.setHospitalId(hospitalContext.requireHospitalId());
    return mapper.toDto(repository.save(entity));
  }

  @Transactional
  public LabTestDTO update(UUID id, LabTestDTO dto) {
    LabTest entity =
        repository.findById(id).orElseThrow(() -> new RuntimeException("Test not found"));

    entity.setTestName(dto.getTestName());
    entity.setTestCode(dto.getTestCode());
    entity.setShortName(dto.getShortName());
    entity.setAliasName(dto.getAliasName());
    entity.setDepartmentId(dto.getDepartmentId());
    entity.setResultType(dto.getResultType());
    entity.setTatValue(dto.getTatValue());
    entity.setTatUnit(dto.getTatUnit());
    entity.setActive(dto.isActive());

    return mapper.toDto(repository.save(entity));
  }

  @Transactional
  public void delete(UUID id) {
    repository.deleteById(id);
  }
}
