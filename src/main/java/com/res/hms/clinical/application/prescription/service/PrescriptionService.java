package com.res.hms.clinical.application.prescription.service;

import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.clinical.domain.Prescription;
import com.res.hms.clinical.domain.PrescriptionItem;
import com.res.hms.clinical.application.prescription.dto.PrescriptionDTO;
import com.res.hms.clinical.application.prescription.dto.PrescriptionItemDTO;
import com.res.hms.clinical.application.prescription.repository.PrescriptionRepository;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

  @Transactional(readOnly = true)
  public PageResponse<PrescriptionDTO> list(int page, int size) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        prescriptionRepository
            .findAllByHospitalId(hospitalId, PageRequest.of(page, size))
            .map(this::toDto));
  }

  private final PrescriptionRepository prescriptionRepository;
  private final MedicineRepository medicineRepository;
  private final AppointmentRepository appointmentRepository;
  private final HospitalContext hospitalContext;

  @Transactional
  public PrescriptionDTO createPrescription(PrescriptionDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    if (dto.getAppointmentId() == null) {
      throw new ApiException(
          "BAD_REQUEST", "Appointment ID is required for prescription", HttpStatus.BAD_REQUEST);
    }

    Appointment appointment =
        appointmentRepository
            .findByIdAndHospitalId(dto.getAppointmentId(), hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Appointment not found", HttpStatus.NOT_FOUND));

    // Check if prescription already exists for this appointment
    if (prescriptionRepository.existsById(appointment.getId())) {
      throw new ApiException(
          "CONFLICT", "Prescription already exists for this appointment", HttpStatus.CONFLICT);
    }

    Prescription prescription = new Prescription();
    prescription.setAppointment(appointment);
    prescription.setPatient(appointment.getPatient());
    prescription.setDoctorProfile(appointment.getDoctorProfile());

    if (dto.getItems() != null) {
      for (PrescriptionItemDTO itemDto : dto.getItems()) {
        Medicine medicine =
            medicineRepository
                .findById(itemDto.getMedicineId())
                .orElseThrow(
                    () ->
                        new ApiException(
                            "NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));

        PrescriptionItem item = new PrescriptionItem();
        item.setPrescription(prescription);
        item.setMedicine(medicine);
        item.setDosage(itemDto.getDosage());
        item.setDuration(itemDto.getDuration());
        prescription.getItems().add(item);
      }
    }

    return toDto(prescriptionRepository.save(prescription));
  }

  @Transactional(readOnly = true)
  public List<PrescriptionDTO> getPatientPrescriptions(UUID patientId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return prescriptionRepository.findByPatientIdAndHospitalId(patientId, hospitalId).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public PrescriptionDTO getById(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return prescriptionRepository
        .findByIdAndHospitalId(id, hospitalId)
        .map(this::toDto)
        .orElseThrow(
            () -> new ApiException("NOT_FOUND", "Prescription not found", HttpStatus.NOT_FOUND));
  }

  public PrescriptionDTO toDto(Prescription prescription) {
    if (prescription == null) {
      return null;
    }

    PrescriptionDTO.PrescriptionDTOBuilder builder =
        PrescriptionDTO.builder().id(prescription.getId());

    if (prescription.getPatient() != null) {
      builder
          .patientId(prescription.getPatient().getId())
          .patientName(prescription.getPatient().getFullName());
    }

    if (prescription.getDoctorProfile() != null) {
      builder
          .doctorId(prescription.getDoctorProfile().getId())
          .doctorName(prescription.getDoctorProfile().getFullName());
    }

    if (prescription.getAppointment() != null) {
      builder
          .appointmentId(prescription.getAppointment().getId())
          .createdAt(prescription.getAppointment().getCreatedAt());
    }

    if (prescription.getItems() != null) {
      builder.items(
          prescription.getItems().stream().map(this::toItemDto).collect(Collectors.toList()));
    }

    return builder.build();
  }

  private PrescriptionItemDTO toItemDto(PrescriptionItem item) {
    if (item == null) {
      return null;
    }
    PrescriptionItemDTO.PrescriptionItemDTOBuilder builder =
        PrescriptionItemDTO.builder()
            .id(item.getId())
            .dosage(item.getDosage())
            .duration(item.getDuration());

    if (item.getMedicine() != null) {
      builder.medicineId(item.getMedicine().getId()).medicineName(item.getMedicine().getName());
    }

    return builder.build();
  }
}
