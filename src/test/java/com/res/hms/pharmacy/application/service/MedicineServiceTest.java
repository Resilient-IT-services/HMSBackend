package com.res.hms.pharmacy.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.application.dto.MedicineDTO;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.MedicineForm;
import com.res.hms.pharmacy.infrastructure.repository.HospitalMedicineCatalogRepository;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

  @Mock private MedicineRepository medicineRepository;
  @Mock private HospitalMedicineCatalogRepository hospitalMedicineCatalogRepository;
  @Mock private PharmacyStockRepository pharmacyStockRepository;
  @Mock private HospitalContext hospitalContext;
  @Mock private PharmacyExcelService pharmacyExcelService;
  @Mock private PharmacyStockService pharmacyStockService;

  @InjectMocks private MedicineService medicineService;

  private Medicine medicine;
  private MedicineDTO medicineDTO;
  private UUID medicineId;
  private UUID hospitalId;

  @BeforeEach
  void setUp() {
    medicineId = UUID.randomUUID();
    hospitalId = UUID.randomUUID();

    medicine = new Medicine();
    medicine.setId(medicineId);
    medicine.setName("Paracetamol");
    medicine.setGenericName("Paracetamol");
    medicine.setThresholdQuantity(10);

    medicineDTO =
        MedicineDTO.builder()
            .name("Paracetamol")
            .genericName("Paracetamol")
            .composition("500mg")
            .strength("500mg")
            .form(MedicineForm.TABLET)
            .manufacturer("GSK")
            .thresholdQuantity(10)
            .build();
  }

  @Test
  void create_ShouldSaveMedicine_WhenNotDuplicate() {
    when(medicineRepository.existsDuplicate(any(), any(), any(), any())).thenReturn(false);
    when(medicineRepository.save(any(Medicine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
    when(hospitalMedicineCatalogRepository.findByHospitalIdAndMedicineId(any(), any()))
        .thenReturn(Optional.empty());

    MedicineDTO result = medicineService.create(medicineDTO);

    assertNotNull(result);
    assertEquals(medicineDTO.getName(), result.getName());
    verify(medicineRepository).save(any(Medicine.class));
  }

  @Test
  void create_ShouldThrowException_WhenDuplicate() {
    when(medicineRepository.existsDuplicate(any(), any(), any(), any())).thenReturn(true);

    assertThrows(ApiException.class, () -> medicineService.create(medicineDTO));
    verify(medicineRepository, never()).save(any(Medicine.class));
  }

  @Test
  void get_ShouldReturnMedicine_WhenExists() {
    when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

    MedicineDTO result = medicineService.get(medicineId);

    assertNotNull(result);
    assertEquals(medicine.getName(), result.getName());
  }

  @Test
  void get_ShouldThrowException_WhenNotExists() {
    when(medicineRepository.findById(medicineId)).thenReturn(Optional.empty());

    assertThrows(ApiException.class, () -> medicineService.get(medicineId));
  }

  @Test
  void update_ShouldUpdateFields_WhenExists() {
    when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
    when(medicineRepository.save(any(Medicine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MedicineDTO result = medicineService.update(medicineId, medicineDTO);

    assertNotNull(result);
    assertEquals(medicineDTO.getName(), result.getName());
    verify(medicineRepository).save(medicine);
  }

  @Test
  void delete_ShouldDelete_WhenExists() {
    when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

    medicineService.delete(medicineId);

    verify(medicineRepository).delete(medicine);
  }

  @Test
  void list_ShouldReturnPageOfDTOs() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Medicine> page = new PageImpl<>(List.of(medicine));
    when(medicineRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    PageResponse<MedicineDTO> result = medicineService.list(pageable, "search", Map.of());

    assertNotNull(result);
    assertEquals(1, result.content().size());
    assertEquals(medicine.getName(), result.content().get(0).getName());
  }
}
