package com.res.hms.ipd.application.service;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.ipd.application.dto.WardDTO;
import com.res.hms.ipd.application.mapper.IpdMapper;
import com.res.hms.ipd.domain.Ward;
import com.res.hms.ipd.infrastructure.repository.WardRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WardService {

  private final WardRepository wardRepository;
  private final IpdMapper ipdMapper;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  @Transactional
  public WardDTO create(WardDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Ward ward = ipdMapper.toEntity(dto);
    ward.setHospitalId(hospitalId);
    return ipdMapper.toDto(wardRepository.save(ward));
  }

  @Transactional
  public WardDTO update(UUID id, WardDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Ward ward =
        resourceLookup.findRequired(id, hospitalId, wardRepository::findByIdAndHospitalId, "Ward");
    ward.setWardName(dto.getWardName());
    ward.setWardType(dto.getWardType());
    ward.setTotalBeds(dto.getTotalBeds());
    ward.setActive(dto.isActive());
    ward.setRemarks(dto.getRemarks());
    return ipdMapper.toDto(wardRepository.save(ward));
  }

  @Transactional(readOnly = true)
  public WardDTO get(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Ward ward =
        resourceLookup.findRequired(id, hospitalId, wardRepository::findByIdAndHospitalId, "Ward");
    return ipdMapper.toDto(ward);
  }

  @Transactional(readOnly = true)
  public PageResponse<WardDTO> list(Pageable pageable) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Page<Ward> wards = wardRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, pageable);
    return PageResponse.from(wards.map(ipdMapper::toDto));
  }
}
