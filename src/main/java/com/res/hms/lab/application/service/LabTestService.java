package com.res.hms.lab.application.service;

import com.res.hms.audit.application.dto.AuditDetail;
import com.res.hms.audit.application.service.AuditLogService;
import com.res.hms.audit.domain.AuditAction;
import com.res.hms.audit.domain.DomainEntity;
import com.res.hms.core.exception.ApiException;
import com.res.hms.lab.application.dto.LabTestDTO;
import com.res.hms.lab.domain.LabTest;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabTestService {

  private final LabTestRepository repository;
  private final LabMapper mapper;
  private final AuditLogService auditLogService;

  @Transactional(readOnly = true)
  public List<LabTestDTO> listAll() {
    return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
  }

  @Transactional
  public LabTestDTO create(LabTestDTO dto) {
    if (repository.existsByTestCodeIgnoreCase(dto.getTestCode())) {
      throw new ApiException(
          "VALIDATION_ERROR", "Global test code must be unique", HttpStatus.BAD_REQUEST);
    }
    if (repository.existsByTestNameIgnoreCase(dto.getTestName())) {
      throw new ApiException(
          "VALIDATION_ERROR", "Global test name must be unique", HttpStatus.BAD_REQUEST);
    }

    LabTest entity = mapper.toEntity(dto);
    LabTest saved = repository.save(entity);

    auditLogService.record(
        null, // Global audit
        DomainEntity.LAB_TEST,
        saved.getId(),
        AuditAction.CREATE,
        new AuditDetail(
            null, java.util.Map.of("message", "Created new Global Lab Test: " + saved.getTestName())));

    return mapper.toDto(saved);
  }

  @Transactional
  public LabTestDTO update(UUID id, LabTestDTO dto) {
    LabTest entity =
        repository.findById(id).orElseThrow(() -> new RuntimeException("Test not found"));

    if (!entity.getTestCode().equalsIgnoreCase(dto.getTestCode())
        && repository.existsByTestCodeIgnoreCase(dto.getTestCode())) {
      throw new ApiException(
          "VALIDATION_ERROR", "Global test code must be unique", HttpStatus.BAD_REQUEST);
    }
    if (!entity.getTestName().equalsIgnoreCase(dto.getTestName())
        && repository.existsByTestNameIgnoreCase(dto.getTestName())) {
      throw new ApiException(
          "VALIDATION_ERROR", "Global test name must be unique", HttpStatus.BAD_REQUEST);
    }

    entity.setTestName(dto.getTestName());
    entity.setTestCode(dto.getTestCode());
    entity.setShortName(dto.getShortName());
    entity.setAliasName(dto.getAliasName());
    entity.setDepartmentId(dto.getDepartmentId());
    entity.setResultType(dto.getResultType());
    entity.setTatValue(dto.getTatValue());
    entity.setTatUnit(dto.getTatUnit());
    entity.setActive(dto.isActive());

    LabTest saved = repository.save(entity);

    auditLogService.record(
        null,
        DomainEntity.LAB_TEST,
        saved.getId(),
        AuditAction.UPDATE,
        new AuditDetail(
            null, java.util.Map.of("message", "Updated Global Lab Test: " + saved.getTestName())));

    return mapper.toDto(saved);
  }

  @Transactional
  public void delete(UUID id) {
    repository
        .findById(id)
        .ifPresent(
            entity -> {
              repository.delete(entity);
              auditLogService.record(
                  null,
                  DomainEntity.LAB_TEST,
                  entity.getId(),
                  AuditAction.DELETE,
                  new AuditDetail(
                      null,
                      java.util.Map.of("message", "Deleted Global Lab Test: " + entity.getTestName())));
            });
  }
}
