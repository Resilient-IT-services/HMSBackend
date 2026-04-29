package com.res.hms.vital.service;

import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.VitalSetting;
import com.res.hms.repository.VitalSettingRepository;
import com.res.hms.vital.dto.VitalSettingDTO;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VitalSettingService {

  private final VitalSettingRepository vitalSettingRepository;
  private final HospitalContext hospitalContext;

  @Transactional(readOnly = true)
  public List<VitalSettingDTO> listSettings() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return vitalSettingRepository.findAllByHospitalId(hospitalId).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public VitalSettingDTO saveSetting(VitalSettingDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    VitalSetting setting =
        vitalSettingRepository
            .findByHospitalIdAndVitalName(hospitalId, dto.getVitalName())
            .orElseGet(
                () -> {
                  VitalSetting s = new VitalSetting();
                  s.setHospitalId(hospitalId);
                  s.setVitalName(dto.getVitalName());
                  return s;
                });

    setting.setMinValue(dto.getMinValue());
    setting.setMaxValue(dto.getMaxValue());
    setting.setUnit(dto.getUnit());

    return toDto(vitalSettingRepository.save(setting));
  }

  private VitalSettingDTO toDto(VitalSetting setting) {
    return VitalSettingDTO.builder()
        .id(setting.getId())
        .vitalName(setting.getVitalName())
        .minValue(setting.getMinValue())
        .maxValue(setting.getMaxValue())
        .unit(setting.getUnit())
        .build();
  }
}
