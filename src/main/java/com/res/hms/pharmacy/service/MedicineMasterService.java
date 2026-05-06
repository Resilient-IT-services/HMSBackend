package com.res.hms.pharmacy.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Medicine;
import com.res.hms.pharmacy.dto.MedicineDTO;
import com.res.hms.pharmacy.mapper.MedicineMapper;
import com.res.hms.pharmacy.repository.MedicineRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicineMasterService {

  private final MedicineRepository medicineRepository;
  private final MedicineMapper medicineMapper;
  private final HospitalContext hospitalContext;

  @Transactional(readOnly = true)
  public PageResponse<MedicineDTO> list(int page, int size, String search) {
    PageRequest pageRequest = PageRequest.of(page, size);
    if (search != null && !search.isBlank()) {
      return PageResponse.from(medicineRepository.searchMaster(search.trim(), pageRequest).map(medicineMapper::toDto));
    }
    return PageResponse.from(medicineRepository.findAllByOrderByCreatedAtDesc(pageRequest).map(medicineMapper::toDto));
  }

  @Transactional(readOnly = true)
  public PageResponse<MedicineDTO> searchAvailable(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        medicineRepository
            .searchAvailableForHospital(hospitalId, search == null ? "" : search.trim(), PageRequest.of(page, size))
            .map(medicineMapper::toDto));
  }

  @Transactional(readOnly = true)
  public MedicineDTO get(UUID id) {
    return medicineMapper.toDto(requireMedicine(id));
  }

  @Transactional
  public MedicineDTO create(MedicineDTO dto) {
    if (medicineRepository.existsDuplicate(
        dto.getName(), dto.getGenericName(), dto.getStrength(), dto.getForm())) {
      throw new ApiException(
          "DUPLICATE_MEDICINE",
          "Medicine with same name, generic name, strength, and form already exists",
          HttpStatus.CONFLICT);
    }
    Medicine medicine = medicineMapper.toEntity(dto);
    medicine = medicineRepository.save(medicine);
    return medicineMapper.toDto(medicine);
  }

  @Transactional
  public MedicineDTO update(UUID id, MedicineDTO dto) {
    Medicine medicine = requireMedicine(id);
    if (!(medicine.getName().equalsIgnoreCase(dto.getName())
        && equalsIgnoreCase(medicine.getGenericName(), dto.getGenericName())
        && equalsIgnoreCase(medicine.getStrength(), dto.getStrength())
        && equalsIgnoreCase(medicine.getForm(), dto.getForm()))) {
      if (medicineRepository.existsDuplicate(
          dto.getName(), dto.getGenericName(), dto.getStrength(), dto.getForm())) {
        throw new ApiException(
            "DUPLICATE_MEDICINE",
            "Medicine with same name, generic name, strength, and form already exists",
            HttpStatus.CONFLICT);
      }
    }
    medicineMapper.updateEntity(dto, medicine);
    return medicineMapper.toDto(medicineRepository.save(medicine));
  }

  @Transactional
  public void delete(UUID id) {
    medicineRepository.delete(requireMedicine(id));
  }

  public Medicine requireMedicine(UUID id) {
    return medicineRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ApiException("NOT_FOUND", "Medicine not found: " + id, HttpStatus.NOT_FOUND));
  }

  private boolean equalsIgnoreCase(String s1, String s2) {
    if (s1 == null || s2 == null) return s1 == s2;
    return s1.equalsIgnoreCase(s2);
  }
}
