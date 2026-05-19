package com.res.hms.hospital.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.hospital.domain.Department;
import com.res.hms.hospital.infrastructure.repository.DepartmentRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentService {

  private final DepartmentRepository departmentRepository;

  @Transactional(readOnly = true)
  public List<String> listActiveNames() {
    return departmentRepository.findAll().stream()
        .filter(Department::isActive)
        .map(Department::getName)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<Department> listAll() {
    return departmentRepository.findAll();
  }

  @Transactional
  public Department create(Department department) {
    if (departmentRepository.findByName(department.getName()).isPresent()) {
      throw new ApiException("ALREADY_EXISTS", "Department already exists", HttpStatus.CONFLICT);
    }
    return departmentRepository.save(department);
  }

  @Transactional
  public Department update(UUID id, Department department) {
    Department existing = departmentRepository.findById(id)
        .orElseThrow(() -> new ApiException("NOT_FOUND", "Department not found", HttpStatus.NOT_FOUND));
    existing.setName(department.getName());
    existing.setDescription(department.getDescription());
    existing.setActive(department.isActive());
    return departmentRepository.save(existing);
  }

  @Transactional
  public void delete(UUID id) {
    departmentRepository.deleteById(id);
  }
}
