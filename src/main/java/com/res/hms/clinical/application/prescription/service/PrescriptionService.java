package com.res.hms.clinical.application.prescription.service;

import com.res.hms.clinical.application.prescription.dto.PrescriptionDTO;
import com.res.hms.clinical.application.prescription.dto.PrescriptionItemDTO;
import com.res.hms.clinical.application.prescription.dto.PrescriptionSummaryDTO;
import com.res.hms.clinical.application.prescription.repository.PrescriptionRepository;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.Prescription;
import com.res.hms.clinical.domain.PrescriptionItem;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.domain.Medicine;
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

  private final PrescriptionRepository prescriptionRepository;
  private final MedicineRepository medicineRepository;
  private final AppointmentRepository appointmentRepository;
  private final HospitalContext hospitalContext;

  @Transactional(readOnly = true)
  public PageResponse<PrescriptionSummaryDTO> listSummary(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);
    if (search != null && !search.trim().isEmpty()) {
      return PageResponse.from(
          prescriptionRepository
              .searchPrescriptions(hospitalId, search.trim(), pageRequest)
              .map(this::toSummaryDto));
    }
    return PageResponse.from(
        prescriptionRepository.findAllByHospitalId(hospitalId, pageRequest).map(this::toSummaryDto));
  }

  @Transactional(readOnly = true)
  public PrescriptionSummaryDTO toSummaryDto(Prescription prescription) {
    if (prescription == null) {
      return null;
    }

    UUID visitId = appointmentRepository.findById(prescription.getAppointmentId())
            .map(Appointment::getVisitId)
            .orElse(null);

    return PrescriptionSummaryDTO.builder()
        .id(prescription.getId())
        .patientId(prescription.getPatientId())
        .patientName(prescription.getPatientName())
        .patientCode(prescription.getPatient() != null ? prescription.getPatient().getPatientCode() : null)
        .doctorId(prescription.getDoctorId())
        .doctorName(prescription.getDoctorName())
        .appointmentId(prescription.getAppointmentId())
        .visitId(visitId)
        .status(prescription.getStatus())
        .createdAt(prescription.getCreatedAt())
        .itemCount(prescription.getItems() != null ? prescription.getItems().size() : 0)
        .build();
  }

  @Transactional(readOnly = true)
  public PageResponse<PrescriptionDTO> list(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);
    if (search != null && !search.trim().isEmpty()) {
      return PageResponse.from(
          prescriptionRepository
              .searchPrescriptions(hospitalId, search.trim(), pageRequest)
              .map(this::toDto));
    }
    return PageResponse.from(
        prescriptionRepository.findAllByHospitalId(hospitalId, pageRequest).map(this::toDto));
  }

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
    if (prescriptionRepository.existsByAppointmentId(appointment.getId())) {
      throw new ApiException(
          "CONFLICT", "Prescription already exists for this appointment", HttpStatus.CONFLICT);
    }

    Prescription prescription = new Prescription();
    prescription.setHospitalId(hospitalId);
    prescription.setAppointmentId(appointment.getId());
    prescription.setPatientId(appointment.getPatientId());
    prescription.setPatientName(appointment.getPatientName());
    prescription.setDoctorId(appointment.getDoctorId());
    prescription.setDoctorName(appointment.getDoctorName());

    if (dto.getItems() != null) {
      for (PrescriptionItemDTO itemDto : dto.getItems()) {
        Medicine medicine =
            medicineRepository
                .findById(itemDto.getMedicineId())
                .orElseThrow(
                    () ->
                        new ApiException("NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));

        PrescriptionItem item = new PrescriptionItem();
        item.setHospitalId(hospitalId);
        item.setPrescription(prescription);
        item.setMedicineId(medicine.getId());
        item.setMedicineName(medicine.getName());
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

    UUID visitId = appointmentRepository.findById(prescription.getAppointmentId())
            .map(Appointment::getVisitId)
            .orElse(null);

    PrescriptionDTO.PrescriptionDTOBuilder builder =
        PrescriptionDTO.builder()
            .id(prescription.getId())
            .patientId(prescription.getPatientId())
            .patientName(prescription.getPatientName())
            .doctorId(prescription.getDoctorId())
            .doctorName(prescription.getDoctorName())
            .appointmentId(prescription.getAppointmentId())
            .visitId(visitId)
            .createdAt(prescription.getCreatedAt());

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
    return PrescriptionItemDTO.builder()
        .id(item.getId())
        .medicineId(item.getMedicineId())
        .medicineName(item.getMedicineName())
        .dosage(item.getDosage())
        .duration(item.getDuration())
        .build();
  }
}
