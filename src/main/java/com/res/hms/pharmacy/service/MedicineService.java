package com.res.hms.pharmacy.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Medicine;
import com.res.hms.pharmacy.dto.MedicineDTO;
import com.res.hms.repository.MedicineRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineService {

  private final MedicineRepository medicineRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  public PageResponse<MedicineDTO> list(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);

    if (search != null && !search.trim().isEmpty()) {
      String searchPattern = "%" + search.trim().toLowerCase() + "%";
      return PageResponse.from(
          medicineRepository
              .findAllByHospitalIdAndNameContainingIgnoreCase(hospitalId, search, pageRequest)
              .map(this::toDto));
    }

    return PageResponse.from(
        medicineRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, pageRequest)
            .map(this::toDto));
  }

  public MedicineDTO get(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return toDto(requireMedicine(id, hospitalId));
  }

  @Transactional
  public MedicineDTO create(MedicineDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Medicine medicine = new Medicine();
    medicine.setHospitalId(hospitalId);
    updateFields(medicine, dto);
    medicine = medicineRepository.save(medicine);
    return toDto(medicine);
  }

  @Transactional
  public MedicineDTO update(UUID id, MedicineDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Medicine medicine = requireMedicine(id, hospitalId);
    updateFields(medicine, dto);
    medicine = medicineRepository.save(medicine);
    return toDto(medicine);
  }

  @Transactional
  public void delete(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Medicine medicine = requireMedicine(id, hospitalId);
    medicineRepository.delete(medicine);
  }

  public Medicine requireMedicine(UUID id, UUID hospitalId) {
    return resourceLookup.findRequired(
        id, hospitalId, medicineRepository::findByIdAndHospitalId, "Medicine");
  }

  private void updateFields(Medicine medicine, MedicineDTO dto) {
    medicine.setName(dto.getName());
    medicine.setComposition(dto.getComposition());
    medicine.setManufacturer(dto.getManufacturer());
  }

  public MedicineDTO toDto(Medicine medicine) {
    return MedicineDTO.builder()
        .id(medicine.getId())
        .name(medicine.getName())
        .composition(medicine.getComposition())
        .manufacturer(medicine.getManufacturer())
        .build();
  }
}
