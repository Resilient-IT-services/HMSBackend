package com.res.hms.tax.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.tax.application.dto.TaxGroupDTO;
import com.res.hms.tax.application.dto.TaxMasterDTO;
import com.res.hms.tax.application.dto.TaxRuleDTO;
import com.res.hms.tax.domain.TaxGroup;
import com.res.hms.tax.domain.TaxGroupDetail;
import com.res.hms.tax.domain.TaxMaster;
import com.res.hms.tax.domain.TaxRule;
import com.res.hms.tax.infrastructure.repository.TaxGroupDetailRepository;
import com.res.hms.tax.infrastructure.repository.TaxGroupRepository;
import com.res.hms.tax.infrastructure.repository.TaxMasterRepository;
import com.res.hms.tax.infrastructure.repository.TaxRuleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaxConfigurationService {

    private final TaxMasterRepository masterRepository;
    private final TaxGroupRepository groupRepository;
    private final TaxGroupDetailRepository detailRepository;
    private final TaxRuleRepository ruleRepository;

    @Transactional(readOnly = true)
    public List<TaxMasterDTO> getAllMasters() {
        return masterRepository.findAll().stream().map(this::toMasterDto).collect(Collectors.toList());
    }

    @Transactional
    public TaxMasterDTO createMaster(TaxMasterDTO dto) {
        TaxMaster master = new TaxMaster();
        updateMasterFields(master, dto);
        return toMasterDto(masterRepository.save(master));
    }

    @Transactional
    public TaxMasterDTO updateMaster(UUID id, TaxMasterDTO dto) {
        TaxMaster master = masterRepository.findById(id)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Tax Master not found", HttpStatus.NOT_FOUND));
        updateMasterFields(master, dto);
        return toMasterDto(masterRepository.save(master));
    }

    @Transactional
    public void deleteMaster(UUID id) {
        if (!masterRepository.existsById(id)) {
            throw new ApiException("NOT_FOUND", "Tax Master not found", HttpStatus.NOT_FOUND);
        }
        masterRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<TaxGroupDTO> getAllGroups() {
        return groupRepository.findAll().stream().map(this::toGroupDto).collect(Collectors.toList());
    }

    @Transactional
    public TaxGroupDTO createGroup(TaxGroupDTO dto) {
        TaxGroup group = new TaxGroup();
        group.setName(dto.getName());
        group.setActive(dto.isActive());
        group = groupRepository.save(group);

        if (dto.getTaxes() != null) {
            for (TaxMasterDTO masterDto : dto.getTaxes()) {
                TaxMaster master = masterRepository.findById(masterDto.getId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Tax Master not found", HttpStatus.NOT_FOUND));
                TaxGroupDetail detail = new TaxGroupDetail();
                detail.setGroup(group);
                detail.setTaxMaster(master);
                detailRepository.save(detail);
            }
        }
        return toGroupDto(groupRepository.findById(group.getId()).get());
    }

    @Transactional
    public TaxGroupDTO updateGroup(UUID id, TaxGroupDTO dto) {
        TaxGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Tax Group not found", HttpStatus.NOT_FOUND));
        group.setName(dto.getName());
        group.setActive(dto.isActive());
        
        detailRepository.deleteByGroup(group);
        if (dto.getTaxes() != null) {
            for (TaxMasterDTO masterDto : dto.getTaxes()) {
                TaxMaster master = masterRepository.findById(masterDto.getId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Tax Master not found", HttpStatus.NOT_FOUND));
                TaxGroupDetail detail = new TaxGroupDetail();
                detail.setGroup(group);
                detail.setTaxMaster(master);
                detailRepository.save(detail);
            }
        }
        return toGroupDto(groupRepository.save(group));
    }

    @Transactional
    public void deleteGroup(UUID id) {
        TaxGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Tax Group not found", HttpStatus.NOT_FOUND));
        detailRepository.deleteByGroup(group);
        groupRepository.delete(group);
    }

    @Transactional(readOnly = true)
    public List<TaxRuleDTO> getAllRules() {
        return ruleRepository.findAll().stream().map(this::toRuleDto).collect(Collectors.toList());
    }

    @Transactional
    public TaxRuleDTO createRule(TaxRuleDTO dto) {
        TaxGroup group = groupRepository.findById(dto.getTaxGroupId())
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Tax Group not found", HttpStatus.NOT_FOUND));

        TaxRule rule = new TaxRule();
        rule.setPriority(dto.getPriority());
        rule.setItemType(dto.getItemType());
        rule.setHsnCode(dto.getHsnCode());
        rule.setSacCode(dto.getSacCode());
        rule.setPatientCategory(dto.getPatientCategory());
        rule.setTaxGroup(group);
        rule.setEffectiveFrom(dto.getEffectiveFrom());
        rule.setEffectiveTo(dto.getEffectiveTo());

        return toRuleDto(ruleRepository.save(rule));
    }

    @Transactional
    public TaxRuleDTO updateRule(UUID id, TaxRuleDTO dto) {
        TaxRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Tax Rule not found", HttpStatus.NOT_FOUND));
        
        TaxGroup group = groupRepository.findById(dto.getTaxGroupId())
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Tax Group not found", HttpStatus.NOT_FOUND));

        rule.setPriority(dto.getPriority());
        rule.setItemType(dto.getItemType());
        rule.setHsnCode(dto.getHsnCode());
        rule.setSacCode(dto.getSacCode());
        rule.setPatientCategory(dto.getPatientCategory());
        rule.setTaxGroup(group);
        rule.setEffectiveFrom(dto.getEffectiveFrom());
        rule.setEffectiveTo(dto.getEffectiveTo());

        return toRuleDto(ruleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(UUID id) {
        if (!ruleRepository.existsById(id)) {
            throw new ApiException("NOT_FOUND", "Tax Rule not found", HttpStatus.NOT_FOUND);
        }
        ruleRepository.deleteById(id);
    }

    private TaxMasterDTO toMasterDto(TaxMaster master) {
        TaxMasterDTO dto = new TaxMasterDTO();
        dto.setId(master.getId());
        dto.setCode(master.getCode());
        dto.setName(master.getName());
        dto.setTaxType(master.getTaxType());
        dto.setRate(master.getRate());
        dto.setActive(master.isActive());
        dto.setEffectiveFrom(master.getEffectiveFrom());
        dto.setEffectiveTo(master.getEffectiveTo());
        return dto;
    }

    private TaxGroupDTO toGroupDto(TaxGroup group) {
        TaxGroupDTO dto = new TaxGroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setActive(group.isActive());
        dto.setTaxes(group.getDetails().stream()
            .map(d -> toMasterDto(d.getTaxMaster()))
            .collect(Collectors.toList()));
        return dto;
    }

    private TaxRuleDTO toRuleDto(TaxRule rule) {
        TaxRuleDTO dto = new TaxRuleDTO();
        dto.setId(rule.getId());
        dto.setPriority(rule.getPriority());
        dto.setItemType(rule.getItemType());
        dto.setHsnCode(rule.getHsnCode());
        dto.setSacCode(rule.getSacCode());
        dto.setPatientCategory(rule.getPatientCategory());
        dto.setTaxGroupId(rule.getTaxGroup().getId());
        dto.setTaxGroupName(rule.getTaxGroup().getName());
        dto.setEffectiveFrom(rule.getEffectiveFrom());
        dto.setEffectiveTo(rule.getEffectiveTo());
        return dto;
    }

    private void updateMasterFields(TaxMaster master, TaxMasterDTO dto) {
        master.setCode(dto.getCode());
        master.setName(dto.getName());
        master.setTaxType(dto.getTaxType());
        master.setRate(dto.getRate());
        master.setActive(dto.isActive());
        master.setEffectiveFrom(dto.getEffectiveFrom());
        master.setEffectiveTo(dto.getEffectiveTo());
    }
}
