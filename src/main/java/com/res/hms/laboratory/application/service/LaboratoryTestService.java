package com.res.hms.laboratory.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.laboratory.application.dto.LaboratoryTestDTO;
import com.res.hms.laboratory.domain.LaboratoryTest;
import com.res.hms.laboratory.infrastructure.repository.LaboratoryTestRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LaboratoryTestService {

  private final LaboratoryTestRepository laboratoryTestRepository;

  @Transactional(readOnly = true)
  public List<LaboratoryTestDTO> listAll() {
    return laboratoryTestRepository.findAll().stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public LaboratoryTestDTO create(LaboratoryTestDTO dto) {
    String trimmedCode = dto.getTestCode().trim();
    String trimmedName = dto.getTestName().trim();

    if (laboratoryTestRepository.existsByTestCodeIgnoreCase(trimmedCode)) {
      throw new ApiException("ALREADY_EXISTS", "Test Code already exists", HttpStatus.CONFLICT);
    }

    if (laboratoryTestRepository.existsByTestNameIgnoreCase(trimmedName)) {
      throw new ApiException("ALREADY_EXISTS", "Test Name already exists", HttpStatus.CONFLICT);
    }

    LaboratoryTest entity = new LaboratoryTest();
    entity.setTestCode(trimmedCode);
    entity.setTestName(trimmedName);
    entity.setShortName(dto.getShortName() != null ? dto.getShortName().trim() : null);
    entity.setAliasName(dto.getAliasName() != null ? dto.getAliasName().trim() : null);
    entity.setActive(true);

    return toDto(laboratoryTestRepository.save(entity));
  }

  public LaboratoryTestDTO toDto(LaboratoryTest entity) {
    return LaboratoryTestDTO.builder()
        .id(entity.getId())
        .testCode(entity.getTestCode())
        .testName(entity.getTestName())
        .shortName(entity.getShortName())
        .aliasName(entity.getAliasName())
        .active(entity.isActive())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
