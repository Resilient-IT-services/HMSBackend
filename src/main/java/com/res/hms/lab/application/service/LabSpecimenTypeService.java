package com.res.hms.lab.application.service;

import com.res.hms.lab.application.dto.LabSpecimenTypeDTO;
import com.res.hms.lab.domain.LabSpecimenType;
import com.res.hms.lab.infrastructure.repository.LabSpecimenTypeRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabSpecimenTypeService {

  private final LabSpecimenTypeRepository repository;

  @Transactional(readOnly = true)
  public List<LabSpecimenTypeDTO> listAll() {
    return repository.findAll().stream().map(this::toDto).collect(Collectors.toList());
  }

  @Transactional
  public LabSpecimenTypeDTO create(LabSpecimenTypeDTO dto) {
    LabSpecimenType entity = LabSpecimenType.builder().code(dto.getCode()).name(dto.getName()).build();
    return toDto(repository.save(entity));
  }

  @Transactional
  public LabSpecimenTypeDTO update(UUID id, LabSpecimenTypeDTO dto) {
    LabSpecimenType entity =
        repository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Specimen type not found: " + id));
    entity.setCode(dto.getCode());
    entity.setName(dto.getName());
    return toDto(repository.save(entity));
  }

  @Transactional
  public void delete(UUID id) {
    repository.deleteById(id);
  }

  private LabSpecimenTypeDTO toDto(LabSpecimenType entity) {
    return LabSpecimenTypeDTO.builder()
        .id(entity.getId())
        .code(entity.getCode())
        .name(entity.getName())
        .build();
  }
}
