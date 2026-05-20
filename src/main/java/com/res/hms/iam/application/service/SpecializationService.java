package com.res.hms.iam.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.iam.domain.Specialization;
import com.res.hms.iam.infrastructure.repository.SpecializationRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpecializationService {

  private final SpecializationRepository specializationRepository;

  @Transactional(readOnly = true)
  public List<String> listActiveNames() {
    return specializationRepository.findAll().stream()
        .filter(Specialization::isActive)
        .map(Specialization::getName)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<Specialization> listAll() {
    return specializationRepository.findAll();
  }

  @Transactional
  public Specialization create(Specialization specialization) {
    if (specializationRepository.findByName(specialization.getName()).isPresent()) {
      throw new ApiException(
          "ALREADY_EXISTS", "Specialization already exists", HttpStatus.CONFLICT);
    }
    return specializationRepository.save(specialization);
  }

  @Transactional
  public Specialization update(UUID id, Specialization specialization) {
    Specialization existing =
        specializationRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ApiException(
                        "NOT_FOUND", "Specialization not found", HttpStatus.NOT_FOUND));
    existing.setName(specialization.getName());
    existing.setDescription(specialization.getDescription());
    existing.setActive(specialization.isActive());
    return specializationRepository.save(existing);
  }

  @Transactional
  public void delete(UUID id) {
    specializationRepository.deleteById(id);
  }
}
