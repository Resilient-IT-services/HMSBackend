package com.res.hms.lab.application.service;

import com.res.hms.lab.application.dto.LabReferenceRangeDTO;
import com.res.hms.lab.domain.LabReferenceRange;
import com.res.hms.lab.infrastructure.repository.LabReferenceRangeRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabReferenceRangeService {

    private final LabReferenceRangeRepository repository;
    private final LabMapper mapper;

    @Transactional(readOnly = true)
    public List<LabReferenceRangeDTO> listByParameter(UUID parameterId) {
        return repository.findAll().stream()
                .filter(r -> r.getParameterId().equals(parameterId))
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public LabReferenceRangeDTO create(LabReferenceRangeDTO dto) {
        LabReferenceRange entity = mapper.toEntity(dto);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public LabReferenceRangeDTO update(UUID id, LabReferenceRangeDTO dto) {
        LabReferenceRange entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reference range not found"));
        
        entity.setGender(dto.getGender());
        entity.setMinAge(dto.getMinAge());
        entity.setMaxAge(dto.getMaxAge());
        entity.setAgeUnit(dto.getAgeUnit());
        entity.setMinValue(dto.getMinValue());
        entity.setMaxValue(dto.getMaxValue());
        entity.setCriticalMin(dto.getCriticalMin());
        entity.setCriticalMax(dto.getCriticalMax());
        
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
