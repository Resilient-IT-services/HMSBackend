package com.res.hms.lab.application.service;

import com.res.hms.lab.application.dto.LabPackageDTO;
import com.res.hms.lab.domain.LabPackage;
import com.res.hms.lab.domain.LabPackageTest;
import com.res.hms.lab.infrastructure.repository.LabPackageRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageTestRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabPackageService {

  private final LabPackageRepository packageRepository;
  private final LabPackageTestRepository packageTestRepository;
  private final LabMapper mapper;

  @Transactional(readOnly = true)
  public List<LabPackageDTO> listAll() {
    return packageRepository.findAll().stream()
        .map(
            p -> {
              LabPackageDTO dto = mapper.toDto(p);
              List<UUID> testIds =
                  packageTestRepository.findByPackageId(p.getId()).stream()
                      .map(LabPackageTest::getTestId)
                      .collect(Collectors.toList());
              dto.setTestIds(testIds);
              return dto;
            })
        .collect(Collectors.toList());
  }

  @Transactional
  public LabPackageDTO create(LabPackageDTO dto) {
    LabPackage entity = mapper.toEntity(dto);
    entity.setVersionNo(1);
    if (entity.getIsBillable() == null) {
      entity.setIsBillable(true);
    }
    entity = packageRepository.save(entity);

    if (dto.getTestIds() != null) {
      for (UUID testId : dto.getTestIds()) {
        packageTestRepository.save(
            LabPackageTest.builder().packageId(entity.getId()).testId(testId).build());
      }
    }

    LabPackageDTO result = mapper.toDto(entity);
    result.setTestIds(dto.getTestIds());
    return result;
  }

  @Transactional
  public LabPackageDTO update(UUID id, LabPackageDTO dto) {
    LabPackage entity =
        packageRepository.findById(id).orElseThrow(() -> new RuntimeException("Package not found"));

    entity.setPackageCode(dto.getPackageCode());
    entity.setPackageName(dto.getPackageName());
    entity.setDescription(dto.getDescription());
    entity.setActive(dto.getActive());
    entity.setPackageType(dto.getPackageType());
    entity.setIsBillable(dto.getIsBillable());
    entity = packageRepository.save(entity);

    packageTestRepository.deleteAll(packageTestRepository.findByPackageId(id));

    if (dto.getTestIds() != null) {
      for (UUID testId : dto.getTestIds()) {
        packageTestRepository.save(LabPackageTest.builder().packageId(id).testId(testId).build());
      }
    }

    LabPackageDTO result = mapper.toDto(entity);
    result.setTestIds(dto.getTestIds());
    return result;
  }

  @Transactional
  public void delete(UUID id) {
    packageRepository.deleteById(id);
    List<LabPackageTest> existingTests =
        packageTestRepository.findAll().stream()
            .filter(pt -> pt.getPackageId().equals(id))
            .collect(Collectors.toList());
    packageTestRepository.deleteAll(existingTests);
  }
}
