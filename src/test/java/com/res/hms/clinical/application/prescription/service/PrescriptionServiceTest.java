package com.res.hms.clinical.application.prescription.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.clinical.application.prescription.dto.PrescriptionDTO;
import com.res.hms.clinical.application.prescription.dto.PrescriptionItemDTO;
import com.res.hms.clinical.application.prescription.dto.PrescriptionLabItemDTO;
import com.res.hms.clinical.application.prescription.dto.PrescriptionSummaryDTO;
import com.res.hms.clinical.application.prescription.repository.PrescriptionRepository;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.domain.Prescription;
import com.res.hms.clinical.domain.PrescriptionItem;
import com.res.hms.clinical.domain.PrescriptionLabItem;
import com.res.hms.clinical.domain.PrescriptionStatus;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
class PrescriptionServiceTest {

  @Mock private PrescriptionRepository prescriptionRepository;
  @Mock private MedicineRepository medicineRepository;
  @Mock private AppointmentRepository appointmentRepository;
  @Mock private HospitalContext hospitalContext;
  @Mock private HospitalRepository hospitalRepository;

  @InjectMocks private PrescriptionService prescriptionService;

  private UUID hospitalId;
  private UUID appointmentId;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    appointmentId = UUID.randomUUID();
  }

  @Test
  @SuppressWarnings("unchecked")
  void testListSummary() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    Pageable pageable = PageRequest.of(0, 10);
    Prescription prescription = new Prescription();
    prescription.setId(UUID.randomUUID());
    prescription.setHospitalId(hospitalId);
    prescription.setItems(new ArrayList<>());

    Page<Prescription> page = new PageImpl<>(List.of(prescription));
    when(prescriptionRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(page);

    PageResponse<PrescriptionSummaryDTO> result =
        prescriptionService.listSummary(pageable, "test", Collections.emptyMap());

    assertNotNull(result);
    assertEquals(1, result.content().size());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testList() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    Pageable pageable = PageRequest.of(0, 10);
    Prescription prescription = new Prescription();
    prescription.setId(UUID.randomUUID());
    prescription.setHospitalId(hospitalId);
    prescription.setItems(new ArrayList<>());

    Page<Prescription> page = new PageImpl<>(List.of(prescription));
    when(prescriptionRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(page);

    PageResponse<PrescriptionDTO> result =
        prescriptionService.list(pageable, "test", Collections.emptyMap());

    assertNotNull(result);
    assertEquals(1, result.content().size());
  }

  @Test
  void testCreatePrescriptionWithMedicines() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);

    Appointment appointment = new Appointment();
    appointment.setId(appointmentId);
    appointment.setStatus(AppointmentStatus.VITALS_RECORDED);
    appointment.setPatientId(UUID.randomUUID());
    appointment.setPatientName("John Doe");
    appointment.setDoctorId(UUID.randomUUID());
    appointment.setDoctorName("Dr. Smith");

    when(appointmentRepository.findByIdAndHospitalId(appointmentId, hospitalId))
        .thenReturn(Optional.of(appointment));
    when(prescriptionRepository.existsByAppointmentId(appointmentId)).thenReturn(false);

    Hospital hospital = new Hospital();
    hospital.setName("Test Hospital");
    when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
    when(prescriptionRepository.nextPrescriptionNumber()).thenReturn(1L);

    UUID medicineId = UUID.randomUUID();
    Medicine medicine = new Medicine();
    medicine.setId(medicineId);
    medicine.setName("Paracetamol");
    when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

    PrescriptionItemDTO itemDto =
        PrescriptionItemDTO.builder()
            .medicineId(medicineId)
            .dosage("500mg")
            .duration("3 days")
            .build();

    PrescriptionItemDTO freeTextDto =
        PrescriptionItemDTO.builder().medicineName("Free Text Med").dosage("10ml").build();

    PrescriptionDTO dto =
        PrescriptionDTO.builder()
            .appointmentId(appointmentId)
            .items(List.of(itemDto, freeTextDto))
            .build();

    when(prescriptionRepository.save(any(Prescription.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PrescriptionDTO result = prescriptionService.createPrescription(dto);

    assertNotNull(result);
    assertEquals(2, result.getItems().size());
    verify(appointmentRepository).save(appointment);
  }

  @Test
  void testCreatePrescriptionErrorCases() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);

    // 1. Missing Appointment ID
    assertThrows(
        ApiException.class,
        () -> prescriptionService.createPrescription(PrescriptionDTO.builder().build()));

    // 2. Appointment Not Found
    when(appointmentRepository.findByIdAndHospitalId(appointmentId, hospitalId))
        .thenReturn(Optional.empty());
    assertThrows(
        ApiException.class,
        () ->
            prescriptionService.createPrescription(
                PrescriptionDTO.builder().appointmentId(appointmentId).build()));

    // 3. Already Exists
    Appointment appointment = new Appointment();
    appointment.setId(appointmentId);
    when(appointmentRepository.findByIdAndHospitalId(appointmentId, hospitalId))
        .thenReturn(Optional.of(appointment));
    when(prescriptionRepository.existsByAppointmentId(appointmentId)).thenReturn(true);
    assertThrows(
        ApiException.class,
        () ->
            prescriptionService.createPrescription(
                PrescriptionDTO.builder().appointmentId(appointmentId).build()));

    // 4. Already Finalized
    when(prescriptionRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
    appointment.setStatus(AppointmentStatus.COMPLETED);
    assertThrows(
        ApiException.class,
        () ->
            prescriptionService.createPrescription(
                PrescriptionDTO.builder().appointmentId(appointmentId).build()));

    // 5. Wrong Status
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    assertThrows(
        ApiException.class,
        () ->
            prescriptionService.createPrescription(
                PrescriptionDTO.builder().appointmentId(appointmentId).build()));
  }

  @Test
  void testSaveOrUpdatePrescriptionEmpty() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    Appointment appointment = new Appointment();
    appointment.setId(appointmentId);

    Prescription existing = new Prescription();
    existing.setItems(new ArrayList<>());
    existing.setLabItems(new ArrayList<>());
    when(prescriptionRepository.findByAppointmentId(appointmentId))
        .thenReturn(Optional.of(existing));

    prescriptionService.saveOrUpdatePrescription(appointment, PrescriptionDTO.builder().build());

    verify(prescriptionRepository).delete(existing);
  }

  @Test
  void testCancelPrescription() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    UUID id = UUID.randomUUID();
    Prescription prescription = new Prescription();
    prescription.setStatus(PrescriptionStatus.PENDING);
    when(prescriptionRepository.findByIdAndHospitalId(id, hospitalId))
        .thenReturn(Optional.of(prescription));

    prescriptionService.cancelPrescription(id);

    assertEquals(PrescriptionStatus.CANCELLED, prescription.getStatus());
    verify(prescriptionRepository).save(prescription);

    // Error case: already cancelled
    assertThrows(ApiException.class, () -> prescriptionService.cancelPrescription(id));
  }

  @Test
  void testGetById() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    UUID id = UUID.randomUUID();
    Prescription prescription = new Prescription();
    prescription.setId(id);
    when(prescriptionRepository.findByIdAndHospitalId(id, hospitalId))
        .thenReturn(Optional.of(prescription));

    PrescriptionDTO result = prescriptionService.getById(id);
    assertNotNull(result);
    assertEquals(id, result.getId());
  }

  @Test
  void testGetPatientPrescriptions() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    UUID patientId = UUID.randomUUID();
    Prescription prescription = new Prescription();
    when(prescriptionRepository.findByPatientIdAndHospitalId(patientId, hospitalId))
        .thenReturn(List.of(prescription));

    List<PrescriptionDTO> results = prescriptionService.getPatientPrescriptions(patientId);
    assertEquals(1, results.size());
  }

  @Test
  void testNullChecks() {
    assertNull(prescriptionService.toDto(null));
    assertNull(prescriptionService.toSummaryDto(null));
    // Test toSummaryDto with null appointment
    Prescription p = new Prescription();
    assertNotNull(prescriptionService.toSummaryDto(p));
  }

  @Test
  void testToDtoWithMedicines() {
    Prescription prescription = new Prescription();
    prescription.setItems(new ArrayList<>());
    PrescriptionItem item = new PrescriptionItem();
    item.setMedicineName("Advil");
    prescription.getItems().add(item);

    PrescriptionDTO dto = prescriptionService.toDto(prescription);
    assertEquals(1, dto.getItems().size());
    assertEquals("Advil", dto.getItems().get(0).getMedicineName());
  }

  @Test
  void testCreatePrescriptionWithLabItems() {
    // Arrange
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);

    Appointment appointment = new Appointment();
    appointment.setId(appointmentId);
    appointment.setStatus(AppointmentStatus.VITALS_RECORDED);
    appointment.setPatientId(UUID.randomUUID());
    appointment.setPatientName("John Doe");
    appointment.setDoctorId(UUID.randomUUID());
    appointment.setDoctorName("Dr. Smith");

    when(appointmentRepository.findByIdAndHospitalId(appointmentId, hospitalId))
        .thenReturn(Optional.of(appointment));
    when(prescriptionRepository.existsByAppointmentId(appointmentId)).thenReturn(false);

    Hospital hospital = new Hospital();
    hospital.setName("Test Hospital");
    when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
    when(prescriptionRepository.nextPrescriptionNumber()).thenReturn(2L);

    PrescriptionLabItemDTO labItemDto =
        PrescriptionLabItemDTO.builder()
            .testId(UUID.randomUUID())
            .testName("Blood Test")
            .notes("Fast for 12 hours")
            .build();

    PrescriptionDTO dto =
        PrescriptionDTO.builder()
            .appointmentId(appointmentId)
            .labItems(List.of(labItemDto))
            .build();

    when(prescriptionRepository.save(any(Prescription.class)))
        .thenAnswer(
            invocation -> {
              Prescription p = invocation.getArgument(0);
              p.setId(UUID.randomUUID());
              return p;
            });

    // Act
    PrescriptionDTO result = prescriptionService.createPrescription(dto);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getLabItems().size());
    assertEquals("Blood Test", result.getLabItems().get(0).getTestName());
    verify(appointmentRepository).save(appointment);
    assertEquals(AppointmentStatus.PRESCRIPTION_FINALIZED, appointment.getStatus());
  }

  @Test
  void testSaveOrUpdatePrescriptionWithLabItems() {
    // Arrange
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);

    Appointment appointment = new Appointment();
    appointment.setId(appointmentId);

    Prescription existingPrescription = new Prescription();
    existingPrescription.setAppointmentId(appointmentId);
    existingPrescription.setLabItems(new ArrayList<>());

    when(prescriptionRepository.findByAppointmentId(appointmentId))
        .thenReturn(Optional.of(existingPrescription));

    PrescriptionLabItemDTO labItemDto =
        PrescriptionLabItemDTO.builder().testId(UUID.randomUUID()).testName("X-Ray").build();

    PrescriptionDTO dto = PrescriptionDTO.builder().labItems(List.of(labItemDto)).build();

    // Act
    prescriptionService.saveOrUpdatePrescription(appointment, dto);

    // Assert
    assertEquals(1, existingPrescription.getLabItems().size());
    assertEquals("X-Ray", existingPrescription.getLabItems().get(0).getTestName());
    verify(prescriptionRepository).save(existingPrescription);
  }

  @Test
  void testToDtoWithLabItems() {
    // Arrange
    Prescription prescription = new Prescription();
    prescription.setId(UUID.randomUUID());

    PrescriptionLabItem labItem = new PrescriptionLabItem();
    labItem.setTestId(UUID.randomUUID());
    labItem.setTestName("MRI");
    labItem.setNotes("Brain scan");

    prescription.setLabItems(new ArrayList<>(List.of(labItem)));

    // Act
    PrescriptionDTO result = prescriptionService.toDto(prescription);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getLabItems().size());
    assertEquals("MRI", result.getLabItems().get(0).getTestName());
    assertEquals("Brain scan", result.getLabItems().get(0).getNotes());
  }
}
