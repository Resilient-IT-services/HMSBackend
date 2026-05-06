package com.res.hms.vital.service;

import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.VitalRangeConfig;
import com.res.hms.domain.VitalType;
import com.res.hms.vital.repository.VitalRangeConfigRepository;
import com.res.hms.vital.dto.VitalSettingDTO;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.vital.mapper.VitalMapper;

@Service
@RequiredArgsConstructor
public class VitalSettingService {

  private final VitalRangeConfigRepository vitalRangeConfigRepository;
  private final HospitalContext hospitalContext;
  private final VitalMapper vitalMapper;

  @Transactional(readOnly = true)
  public List<VitalSettingDTO> listSettings() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return vitalRangeConfigRepository.findAllByHospitalId(hospitalId).stream()
        .map(vitalMapper::toSettingDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public VitalSettingDTO saveSetting(VitalSettingDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    VitalType type;
    try {
      type = VitalType.valueOf(dto.getVitalName());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new com.res.hms.common.exception.ApiException(
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

    vitalMapper.updateEntity(dto, setting);

    return vitalMapper.toSettingDto(vitalRangeConfigRepository.save(setting));
  }
}
