package com.res.hms.lab.application.service;

import com.res.hms.lab.application.dto.LabTestParameterDTO;
import com.res.hms.lab.domain.LabTestParameter;
import com.res.hms.lab.infrastructure.repository.LabTestParameterRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabTestParameterService {

    private final LabTestParameterRepository repository;
    private final LabMapper mapper;

    @Transactional(readOnly = true)
    public List<LabTestParameterDTO> listByTest(UUID testId) {
        return repository.findAll().stream()
                .filter(p -> p.getTestId().equals(testId))
                .sorted((a, b) -> (a.getDisplayOrder() != null && b.getDisplayOrder() != null) 
                        ? a.getDisplayOrder().compareTo(b.getDisplayOrder()) : 0)
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public LabTestParameterDTO create(LabTestParameterDTO dto) {
        LabTestParameter entity = mapper.toEntity(dto);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public LabTestParameterDTO update(UUID id, LabTestParameterDTO dto) {
        LabTestParameter entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parameter not found"));
        
        entity.setParameterName(dto.getParameterName());
        entity.setUnit(dto.getUnit());
        entity.setDisplayOrder(dto.getDisplayOrder());
        
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
