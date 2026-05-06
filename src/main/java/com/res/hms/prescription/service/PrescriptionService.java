package com.res.hms.prescription.service;

import com.res.hms.appointment.repository.AppointmentRepository;
import com.res.hms.common.api.PageResponse;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Appointment;
import com.res.hms.domain.Medicine;
import com.res.hms.domain.Prescription;
import com.res.hms.domain.PrescriptionItem;
import com.res.hms.prescription.dto.PrescriptionDTO;
import com.res.hms.prescription.dto.PrescriptionItemDTO;
import com.res.hms.prescription.repository.PrescriptionRepository;
import com.res.hms.pharmacy.repository.MedicineRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.prescription.mapper.PrescriptionMapper;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

  private final PrescriptionRepository prescriptionRepository;
  private final MedicineRepository medicineRepository;
  private final AppointmentRepository appointmentRepository;
  private final HospitalContext hospitalContext;
  private final PrescriptionMapper prescriptionMapper;

  @Transactional(readOnly = true)
  public PageResponse<PrescriptionDTO> list(int page, int size) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        prescriptionRepository
            .findAllByHospitalId(hospitalId, PageRequest.of(page, size))
            .map(prescriptionMapper::toDto));
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

        PrescriptionItem item = prescriptionMapper.toEntity(itemDto);
        item.setPrescription(prescription);
        item.setMedicine(medicine);
        prescription.getItems().add(item);
      }
    }

    return prescriptionMapper.toDto(prescriptionRepository.save(prescription));
  }

  @Transactional(readOnly = true)
  public List<PrescriptionDTO> getPatientPrescriptions(UUID patientId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return prescriptionRepository.findByPatientIdAndHospitalId(patientId, hospitalId).stream()
        .map(prescriptionMapper::toDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public PrescriptionDTO getById(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return prescriptionRepository
        .findByIdAndHospitalId(id, hospitalId)
        .map(prescriptionMapper::toDto)
        .orElseThrow(
            () -> new ApiException("NOT_FOUND", "Prescription not found", HttpStatus.NOT_FOUND));
  }

  public PrescriptionDTO toDto(Prescription prescription) {
    return prescriptionMapper.toDto(prescription);
  }
}
