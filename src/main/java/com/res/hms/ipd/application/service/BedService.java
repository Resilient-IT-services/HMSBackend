package com.res.hms.ipd.application.service;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.ipd.application.dto.BedDTO;
import com.res.hms.ipd.application.mapper.IpdMapper;
import com.res.hms.ipd.domain.Bed;
import com.res.hms.ipd.domain.BedStatus;
import com.res.hms.ipd.infrastructure.repository.BedRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BedService {

  private final BedRepository bedRepository;
  private final IpdMapper ipdMapper;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  @Transactional
  public BedDTO create(BedDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Bed bed = ipdMapper.toEntity(dto);
    bed.setHospitalId(hospitalId);
    bed.setStatus(BedStatus.AVAILABLE);
    return ipdMapper.toDto(bedRepository.save(bed));
  }

  @Transactional
  public BedDTO update(UUID id, BedDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Bed bed =
        resourceLookup.findRequired(id, hospitalId, bedRepository::findByIdAndHospitalId, "Bed");
    bed.setBedNumber(dto.getBedNumber());
    bed.setActive(dto.isActive());
    return ipdMapper.toDto(bedRepository.save(bed));
  }

  @Transactional(readOnly = true)
  public BedDTO get(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Bed bed =
        resourceLookup.findRequired(id, hospitalId, bedRepository::findByIdAndHospitalId, "Bed");
    return ipdMapper.toDto(bed);
  }

  @Transactional(readOnly = true)
  public PageResponse<BedDTO> list(Pageable pageable) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Page<Bed> beds = bedRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, pageable);
    return PageResponse.from(beds.map(ipdMapper::toDto));
  }

  @Transactional(readOnly = true)
  public List<BedDTO> listByWard(UUID wardId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return bedRepository.findAllByWardIdAndHospitalId(wardId, hospitalId).stream()
        .map(ipdMapper::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public void updateStatus(UUID bedId, BedStatus status) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Bed bed =
        resourceLookup.findRequired(bedId, hospitalId, bedRepository::findByIdAndHospitalId, "Bed");
    bed.setStatus(status);
    bedRepository.save(bed);
  }
}
