package com.res.hms.vital.service;

import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.VitalRangeConfig;
import com.res.hms.domain.VitalType;
import com.res.hms.repository.VitalRangeConfigRepository;
import com.res.hms.vital.dto.VitalRangeConfigDTO;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VitalRangeConfigService {

  private final VitalRangeConfigRepository repository;
  private final HospitalContext hospitalContext;

  @Transactional(readOnly = true)
  public List<VitalRangeConfigDTO> listConfigs() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return repository.findAllByHospitalId(hospitalId).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public VitalRangeConfigDTO updateConfig(VitalType type, VitalRangeConfigDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    VitalRangeConfig config =
        repository
            .findByHospitalIdAndVitalType(hospitalId, type)
            .orElseGet(
                () -> {
                  VitalRangeConfig c = new VitalRangeConfig();
                  c.setHospitalId(hospitalId);
                  c.setVitalType(type);
                  return c;
                });

    config.setMinValue(dto.getMinValue());
    config.setMaxValue(dto.getMaxValue());
    config.setUnit(dto.getUnit());

    return toDto(repository.save(config));
  }

  private VitalRangeConfigDTO toDto(VitalRangeConfig config) {
    return VitalRangeConfigDTO.builder()
        .id(config.getId())
        .vitalType(config.getVitalType())
        .minValue(config.getMinValue())
        .maxValue(config.getMaxValue())
        .unit(config.getUnit())
        .build();
  }
}
