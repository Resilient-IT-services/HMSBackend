package com.res.hms.clinical.application.vital.service;

import com.res.hms.clinical.application.vital.dto.VitalSettingDTO;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.hospital.domain.VitalRangeConfig;
import com.res.hms.hospital.domain.VitalType;
import com.res.hms.hospital.infrastructure.repository.VitalRangeConfigRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VitalSettingService {

  private final VitalRangeConfigRepository vitalRangeConfigRepository;
  private final HospitalContext hospitalContext;

  @Transactional(readOnly = true)
  public List<VitalSettingDTO> listSettings() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return vitalRangeConfigRepository.findAllByHospitalId(hospitalId).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public VitalSettingDTO saveSetting(VitalSettingDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    VitalType type;
    try {
      type = VitalType.valueOf(dto.getVitalName());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new com.res.hms.core.exception.ApiException(
          "INVALID_VITAL_TYPE",
          "Invalid vital type: " + dto.getVitalName(),
          org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    VitalRangeConfig setting =
        vitalRangeConfigRepository
            .findByHospitalIdAndVitalType(hospitalId, type)
            .orElseGet(
                () -> {
                  VitalRangeConfig s = new VitalRangeConfig();
                  s.setHospitalId(hospitalId);
                  s.setVitalType(type);
                  return s;
                });

    setting.setMinValue(dto.getMinValue());
    setting.setMaxValue(dto.getMaxValue());
    setting.setUnit(dto.getUnit());

    return toDto(vitalRangeConfigRepository.save(setting));
  }

  private VitalSettingDTO toDto(VitalRangeConfig setting) {
    return VitalSettingDTO.builder()
        .id(setting.getId())
        .vitalName(setting.getVitalType().name())
        .minValue(setting.getMinValue())
        .maxValue(setting.getMaxValue())
        .unit(setting.getUnit())
        .build();
  }
}
