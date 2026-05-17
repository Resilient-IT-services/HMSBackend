package com.res.hms.clinical.application.vital.service;

import com.res.hms.clinical.application.vital.dto.VitalRangeConfigDTO;
import com.res.hms.hospital.domain.VitalRangeConfig;
import com.res.hms.hospital.domain.VitalType;
import com.res.hms.hospital.infrastructure.repository.VitalRangeConfigRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VitalRangeConfigService {

  private final VitalRangeConfigRepository repository;

  @Transactional(readOnly = true)
  public List<VitalRangeConfigDTO> listConfigs() {
    return repository.findAll().stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public VitalRangeConfigDTO updateConfig(VitalType type, VitalRangeConfigDTO dto) {
    VitalRangeConfig config =
        repository
            .findByVitalType(type)
            .orElseGet(
                () -> {
                  VitalRangeConfig c = new VitalRangeConfig();
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
