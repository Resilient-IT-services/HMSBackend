package com.res.hms.lab.application.service;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.lab.application.dto.LabMachineDTO;
import com.res.hms.lab.domain.LabMachine;
import com.res.hms.lab.infrastructure.repository.LabMachineRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabMachineService {

  private final LabMachineRepository machineRepository;
  private final HospitalContext hospitalContext;
  private final LabMapper mapper;

  @Transactional(readOnly = true)
  public List<LabMachineDTO> listAll() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return machineRepository.findAll().stream()
        .filter(m -> m.getHospitalId().equals(hospitalId))
        .map(mapper::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public LabMachineDTO create(LabMachineDTO dto) {
    LabMachine entity = mapper.toEntity(dto);
    entity.setHospitalId(hospitalContext.requireHospitalId());
    entity = machineRepository.save(entity);
    return mapper.toDto(entity);
  }

  @Transactional
  public LabMachineDTO update(UUID id, LabMachineDTO dto) {
    LabMachine entity =
        machineRepository.findById(id).orElseThrow(() -> new RuntimeException("Machine not found"));

    entity.setMachineCode(dto.getMachineCode());
    entity.setMachineName(dto.getMachineName());
    entity.setManufacturer(dto.getManufacturer());
    entity.setSerialNumber(dto.getSerialNumber());
    entity = machineRepository.save(entity);
    return mapper.toDto(entity);
  }

  @Transactional
  public void delete(UUID id) {
    machineRepository.deleteById(id);
  }
}
