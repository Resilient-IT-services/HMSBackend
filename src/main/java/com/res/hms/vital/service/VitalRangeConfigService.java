package com.res.hms.vital.service;

import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.VitalRangeConfig;
import com.res.hms.domain.VitalType;
import com.res.hms.vital.repository.VitalRangeConfigRepository;
import com.res.hms.vital.dto.VitalRangeConfigDTO;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.vital.mapper.VitalMapper;

@Service
@RequiredArgsConstructor
public class VitalRangeConfigService {

  private final VitalRangeConfigRepository repository;
  private final HospitalContext hospitalContext;
  private final VitalMapper vitalMapper;

  @Transactional(readOnly = true)
  public List<VitalRangeConfigDTO> listConfigs() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return repository.findAllByHospitalId(hospitalId).stream()
        .map(vitalMapper::toDto)
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

    vitalMapper.updateEntity(dto, config);
    return vitalMapper.toDto(repository.save(config));
  }
}
