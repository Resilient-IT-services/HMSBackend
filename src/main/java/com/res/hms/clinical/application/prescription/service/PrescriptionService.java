package com.res.hms.clinical.application.prescription.service;

import com.res.hms.clinical.application.prescription.dto.PrescriptionDTO;
import com.res.hms.clinical.application.prescription.dto.PrescriptionItemDTO;
import com.res.hms.clinical.application.prescription.dto.PrescriptionSummaryDTO;
import com.res.hms.clinical.application.prescription.repository.PrescriptionRepository;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.domain.Prescription;
import com.res.hms.clinical.domain.PrescriptionItem;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

  private Specification<Prescription> getFilterSpec(UUID hospitalId, String search) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
      predicates.add(cb.equal(root.get("deleted"), false));

      if (search != null && !search.trim().isEmpty()) {
        String pattern = "%" + search.trim().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("patientName")), pattern),
                cb.like(cb.lower(root.get("doctorName")), pattern),
                cb.like(cb.lower(root.get("id").as(String.class)), pattern)));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  @Transactional(readOnly = true)
  public PageResponse<PrescriptionSummaryDTO> listSummary(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Specification<Prescription> spec = getFilterSpec(hospitalId, search);
    Specification<Prescription> fetchSpec =
        (root, query, cb) -> {
          if (query.getResultType() != Long.class && query.getResultType() != long.class) {
            root.fetch("patient", JoinType.LEFT);
            root.fetch("appointment", JoinType.LEFT);
          }
          return null;
        };

    Page<Prescription> results =
        prescriptionRepository.findAll(Specification.where(spec).and(fetchSpec), pageable);
    return PageResponse.from(results.map(this::toSummaryDto));
  }

  @Transactional(readOnly = true)
  public PrescriptionSummaryDTO toSummaryDto(Prescription prescription) {
    if (prescription == null) {
      return null;
    }

    UUID visitId =
        prescription.getAppointment() != null ? prescription.getAppointment().getVisitId() : null;

    return PrescriptionSummaryDTO.builder()
        .id(prescription.getId())
        .patientId(prescription.getPatientId())
        .patientName(prescription.getPatientName())
        .patientCode(
            prescription.getPatient() != null ? prescription.getPatient().getPatientCode() : null)
        .doctorId(prescription.getDoctorId())
        .doctorName(prescription.getDoctorName())
        .appointmentId(prescription.getAppointmentId())
        .visitId(visitId)
        .createdAt(prescription.getCreatedAt())
        .itemCount(prescription.getItems() != null ? prescription.getItems().size() : 0)
        .build();
  }

  @Transactional(readOnly = true)
  public PageResponse<PrescriptionDTO> list(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Specification<Prescription> spec = getFilterSpec(hospitalId, search);
    Specification<Prescription> fetchSpec =
        (root, query, cb) -> {
          if (query.getResultType() != Long.class && query.getResultType() != long.class) {
            root.fetch("patient", JoinType.LEFT);
            root.fetch("items", JoinType.LEFT);
            root.fetch("appointment", JoinType.LEFT);
          }
          return null;
        };

    Page<Prescription> results =
        prescriptionRepository.findAll(Specification.where(spec).and(fetchSpec), pageable);
    return PageResponse.from(results.map(this::toDto));
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

    // Preserve conflict semantics for duplicate creation attempts on the same appointment.
    if (prescriptionRepository.existsByAppointmentId(appointment.getId())) {
      throw new ApiException(
          "CONFLICT", "Prescription already exists for this appointment", HttpStatus.CONFLICT);
    }

    if (appointment.getStatus() == AppointmentStatus.PRESCRIPTION_FINALIZED
        || appointment.getStatus() == AppointmentStatus.COMPLETED) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION",
          "Prescription is finalized and cannot be modified.",
          HttpStatus.BAD_REQUEST);
    }

    if (appointment.getStatus() != AppointmentStatus.VITALS_RECORDED
        && appointment.getStatus() != AppointmentStatus.CONSULTATION_STARTED) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION",
          "Prescription can only be created after vitals are recorded.",
          HttpStatus.BAD_REQUEST);
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
        PrescriptionItem item = new PrescriptionItem();
        item.setHospitalId(hospitalId);
        item.setPrescription(prescription);
        item.setDosage(itemDto.getDosage());
        item.setDuration(itemDto.getDuration());

        if (itemDto.getMedicineId() != null) {
          Medicine medicine =
              medicineRepository
                  .findById(itemDto.getMedicineId())
                  .orElseThrow(
                      () ->
                          new ApiException(
                              "NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));
          item.setMedicineId(medicine.getId());
          item.setMedicineName(medicine.getName());
        } else {
          item.setMedicineId(null);
          item.setMedicineName(itemDto.getMedicineName());
        }
        prescription.getItems().add(item);
      }
    }

    appointment.setStatus(AppointmentStatus.PRESCRIPTION_FINALIZED);
    appointmentRepository.save(appointment);

    return toDto(prescriptionRepository.save(prescription));
  }

  @Transactional
  public void saveOrUpdatePrescription(Appointment appointment, PrescriptionDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    Prescription prescription =
        prescriptionRepository
            .findByAppointmentId(appointment.getId())
            .orElseGet(
                () -> {
                  Prescription p = new Prescription();
                  p.setHospitalId(hospitalId);
                  p.setAppointmentId(appointment.getId());
                  p.setPatientId(appointment.getPatientId());
                  p.setPatientName(appointment.getPatientName());
                  p.setDoctorId(appointment.getDoctorId());
                  p.setDoctorName(appointment.getDoctorName());
                  return p;
                });

    // Clear existing items and re-add from DTO for simplicity in "upsert"
    prescription.getItems().clear();

    if (dto.getItems() != null) {
      for (PrescriptionItemDTO itemDto : dto.getItems()) {
        PrescriptionItem item = new PrescriptionItem();
        item.setHospitalId(hospitalId);
        item.setPrescription(prescription);
        item.setDosage(itemDto.getDosage());
        item.setDuration(itemDto.getDuration());

        if (itemDto.getMedicineId() != null) {
          Medicine medicine =
              medicineRepository
                  .findById(itemDto.getMedicineId())
                  .orElseThrow(
                      () ->
                          new ApiException(
                              "NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));
          item.setMedicineId(medicine.getId());
          item.setMedicineName(medicine.getName());
        } else {
          item.setMedicineId(null);
          item.setMedicineName(itemDto.getMedicineName());
        }
        prescription.getItems().add(item);
      }
    }

    prescriptionRepository.save(prescription);
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

    UUID visitId =
        prescription.getAppointment() != null ? prescription.getAppointment().getVisitId() : null;

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
