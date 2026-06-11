package com.res.hms.lab.application.service;

import com.res.hms.lab.application.dto.LabDepartmentDTO;
import com.res.hms.lab.domain.LabDepartment;
import com.res.hms.lab.infrastructure.repository.LabDepartmentRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabDepartmentService {

  private final LabDepartmentRepository repository;
  private final LabMapper mapper;

  @Transactional(readOnly = true)
  public List<LabDepartmentDTO> listAll() {
    return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
  }

  @Transactional
  public LabDepartmentDTO create(LabDepartmentDTO dto) {
    LabDepartment entity = mapper.toEntity(dto);
    return mapper.toDto(repository.save(entity));
  }

  @Transactional
  public LabDepartmentDTO update(UUID id, LabDepartmentDTO dto) {
    LabDepartment entity =
        repository.findById(id).orElseThrow(() -> new RuntimeException("Department not found"));

    entity.setName(dto.getName());
    entity.setCode(dto.getCode());
    entity.setActive(dto.isActive());

    return mapper.toDto(repository.save(entity));
  }

  @Transactional
  public void delete(UUID id) {
    repository.deleteById(id);
  }
}
