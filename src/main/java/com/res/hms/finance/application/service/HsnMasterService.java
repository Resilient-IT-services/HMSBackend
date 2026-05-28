package com.res.hms.finance.application.service;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.finance.application.dto.HsnMasterDTO;
import com.res.hms.finance.domain.HsnMaster;
import com.res.hms.finance.infrastructure.repository.HsnMasterRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HsnMasterService {

  private final HsnMasterRepository hsnMasterRepository;
  private final HospitalContext hospitalContext;

  @Transactional(readOnly = true)
  public List<HsnMasterDTO> getAll() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return hsnMasterRepository.findAllByHospitalIdAndActiveTrue(hospitalId).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public HsnMasterDTO createOrUpdate(HsnMasterDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    HsnMaster hsnMaster;

    if (dto.getId() != null) {
      hsnMaster =
          hsnMasterRepository
              .findById(dto.getId())
              .orElseThrow(() -> new RuntimeException("HSN Master not found"));
    } else {
      hsnMaster =
          hsnMasterRepository
              .findByHospitalIdAndHsnCodeAndActiveTrue(hospitalId, dto.getHsnCode())
              .orElseGet(
                  () -> {
                    HsnMaster newMaster = new HsnMaster();
                    newMaster.setHospitalId(hospitalId);
                    return newMaster;
                  });
    }

    hsnMaster.setHsnCode(dto.getHsnCode());
    hsnMaster.setDescription(dto.getDescription());
    hsnMaster.setTaxPercentage(dto.getTaxPercentage());
    hsnMaster.setActive(dto.isActive());

    return toDto(hsnMasterRepository.save(hsnMaster));
  }

  @Transactional
  public void delete(UUID id) {
    hsnMasterRepository
        .findById(id)
        .ifPresent(
            hsn -> {
              hsn.setActive(false);
              hsnMasterRepository.save(hsn);
            });
  }

  private HsnMasterDTO toDto(HsnMaster hsn) {
    return HsnMasterDTO.builder()
        .id(hsn.getId())
        .hsnCode(hsn.getHsnCode())
        .description(hsn.getDescription())
        .taxPercentage(hsn.getTaxPercentage())
        .active(hsn.isActive())
        .build();
  }
}
