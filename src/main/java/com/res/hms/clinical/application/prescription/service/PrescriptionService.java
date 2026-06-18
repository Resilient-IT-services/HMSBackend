package com.res.hms.clinical.application.prescription.service;

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
import com.res.hms.clinical.domain.PrescriptionLabItemStatus;
import com.res.hms.clinical.domain.PrescriptionStatus;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
  private final HospitalRepository hospitalRepository;

  @Transactional
  public String generatePrescriptionNumber() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    com.res.hms.hospital.domain.Hospital hospital = hospitalRepository.findById(hospitalId)
        .orElseThrow(() -> new ApiException("NOT_FOUND", "Hospital not found", HttpStatus.NOT_FOUND));
    String initials = extractInitials(hospital.getName());
    int twoDigitYear = LocalDate.now().getYear() % 100;
    long seq = prescriptionRepository.nextPrescriptionNumber();
    return String.format("%s-%02d-%05d", initials, twoDigitYear, seq);
  }

  private String extractInitials(String name) {
    if (name == null || name.isBlank()) return "RX";
    return Arrays.stream(name.trim().split("\\s+"))
        .filter(w -> !w.isEmpty())
        .map(w -> String.valueOf(w.charAt(0)).toUpperCase())
        .collect(Collectors.joining());
  }

  @Transactional(readOnly = true)
  public PageResponse<PrescriptionSummaryDTO> listSummary(
      org.springframework.data.domain.Pageable pageable,
      String search,
      java.util.Map<String, String> filters) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    com.res.hms.core.util.SpecificationBuilder<Prescription> builder =
        new com.res.hms.core.util.SpecificationBuilder<>(
            filters, search, java.util.List.of("patientName", "doctorName", "prescriptionNumber"));

    Specification<Prescription> spec = builder.build();
    Specification<Prescription> fixedSpec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
          predicates.add(cb.equal(root.get("deleted"), false));
          // Only show prescriptions with items in the pharmacy queue
          predicates.add(cb.greaterThan(cb.size(root.get("items")), 0));
          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Specification<Prescription> fetchSpec =
        (root, query, cb) -> {
          if (query.getResultType() != Long.class && query.getResultType() != long.class) {
            root.fetch("patient", JoinType.LEFT);
            root.fetch("appointment", JoinType.LEFT);
          }
          return null;
        };

    Page<Prescription> results =
        prescriptionRepository.findAll(
            Specification.where(spec).and(fixedSpec).and(fetchSpec), pageable);
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
        .prescriptionNumber(prescription.getPrescriptionNumber())
        .patientId(prescription.getPatientId())
        .patientName(prescription.getPatientName())
        .patientCode(
            prescription.getPatient() != null ? prescription.getPatient().getPatientCode() : null)
        .doctorId(prescription.getDoctorId())
        .doctorName(prescription.getDoctorName())
        .appointmentId(prescription.getAppointmentId())
        .visitId(visitId)
        .status(prescription.getStatus() != null ? prescription.getStatus().name() : null)
        .createdAt(prescription.getCreatedAt())
        .itemCount(prescription.getItems() != null ? prescription.getItems().size() : 0)
        .build();
  }

  @Transactional(readOnly = true)
  public PageResponse<PrescriptionDTO> list(
      org.springframework.data.domain.Pageable pageable,
      String search,
      java.util.Map<String, String> filters) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    com.res.hms.core.util.SpecificationBuilder<Prescription> builder =
        new com.res.hms.core.util.SpecificationBuilder<>(
            filters, search, java.util.List.of("patientName", "doctorName", "prescriptionNumber"));

    Specification<Prescription> spec = builder.build();
    Specification<Prescription> fixedSpec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
          predicates.add(cb.equal(root.get("deleted"), false));
          // Only show prescriptions with items in the pharmacy queue
          predicates.add(cb.greaterThan(cb.size(root.get("items")), 0));
          return cb.and(predicates.toArray(new Predicate[0]));
        };

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
        prescriptionRepository.findAll(
            Specification.where(spec).and(fixedSpec).and(fetchSpec), pageable);
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
    prescription.setPrescriptionNumber(generatePrescriptionNumber());

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

    if (dto.getLabItems() != null) {
      for (PrescriptionLabItemDTO labDto : dto.getLabItems()) {
        PrescriptionLabItem labItem = new PrescriptionLabItem();
        labItem.setHospitalId(hospitalId);
        labItem.setPrescription(prescription);
        labItem.setTestId(labDto.getTestId());
        labItem.setTestName(labDto.getTestName());
        labItem.setNotes(labDto.getNotes());
        labItem.setStatus(PrescriptionLabItemStatus.PENDING);
        prescription.getLabItems().add(labItem);
      }
    }

    appointment.setStatus(AppointmentStatus.PRESCRIPTION_FINALIZED);
    appointmentRepository.save(appointment);

    return toDto(prescriptionRepository.save(prescription));
  }

  @Transactional
  public void saveOrUpdatePrescription(Appointment appointment, PrescriptionDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    // If both items and labItems are empty or null, we shouldn't have a prescription record
    boolean hasItems = dto.getItems() != null && !dto.getItems().isEmpty();
    boolean hasLabItems = dto.getLabItems() != null && !dto.getLabItems().isEmpty();

    if (!hasItems && !hasLabItems) {
      prescriptionRepository
          .findByAppointmentId(appointment.getId())
          .ifPresent(
              p -> {
                p.getItems().clear();
                p.getLabItems().clear();
                prescriptionRepository.delete(p);
              });
      return;
    }

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
                  p.setPrescriptionNumber(generatePrescriptionNumber());
                  return p;
                });

    // Clear existing items and re-add from DTO for simplicity in "upsert"
    prescription.getItems().clear();

    // Preserve lab items that are already ordered, only update pending ones
    List<PrescriptionLabItem> orderedLabItems =
        prescription.getLabItems().stream()
            .filter(item -> item.getStatus() == PrescriptionLabItemStatus.ORDERED)
            .collect(Collectors.toList());

    prescription.getLabItems().clear();
    prescription.getLabItems().addAll(orderedLabItems);

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

    if (dto.getLabItems() != null) {
      for (PrescriptionLabItemDTO labDto : dto.getLabItems()) {
        // Skip if this test was already ordered (to prevent duplicates)
        boolean alreadyOrdered =
            orderedLabItems.stream()
                .anyMatch(ordered -> ordered.getTestId().equals(labDto.getTestId()));
        if (alreadyOrdered) {
          continue;
        }

        PrescriptionLabItem labItem = new PrescriptionLabItem();
        labItem.setHospitalId(hospitalId);
        labItem.setPrescription(prescription);
        labItem.setTestId(labDto.getTestId());
        labItem.setTestName(labDto.getTestName());
        labItem.setNotes(labDto.getNotes());
        labItem.setStatus(PrescriptionLabItemStatus.PENDING);
        prescription.getLabItems().add(labItem);
      }
    }

    prescriptionRepository.save(prescription);
  }

  @Transactional(readOnly = true)
  public PageResponse<PrescriptionSummaryDTO> listLabRecommendations(
      org.springframework.data.domain.Pageable pageable, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    Specification<Prescription> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
          predicates.add(cb.equal(root.get("deleted"), false));

          // Only show prescriptions with pending lab items
          jakarta.persistence.criteria.Join<Prescription, PrescriptionLabItem> labItems =
              root.join("labItems");
          predicates.add(cb.equal(labItems.get("status"), PrescriptionLabItemStatus.PENDING));

          if (search != null && !search.isEmpty()) {
            String searchLower = "%" + search.toLowerCase() + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.lower(root.get("patientName")), searchLower),
                    cb.like(cb.lower(root.get("doctorName")), searchLower)));
          }

          query.distinct(true);
          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Page<Prescription> results = prescriptionRepository.findAll(spec, pageable);
    return PageResponse.from(results.map(this::toSummaryDto));
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

  @Transactional
  public void cancelPrescription(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Prescription prescription =
        prescriptionRepository
            .findByIdAndHospitalId(id, hospitalId)
            .orElseThrow(
                () ->
                    new ApiException("NOT_FOUND", "Prescription not found", HttpStatus.NOT_FOUND));

    if (prescription.getStatus() != PrescriptionStatus.PENDING) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION",
          "Only pending prescriptions can be cancelled. Current status: "
              + prescription.getStatus(),
          HttpStatus.BAD_REQUEST);
    }

    prescription.setStatus(PrescriptionStatus.CANCELLED);
    prescriptionRepository.save(prescription);
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
            .prescriptionNumber(prescription.getPrescriptionNumber())
            .patientId(prescription.getPatientId())
            .patientName(prescription.getPatientName())
            .doctorId(prescription.getDoctorId())
            .doctorName(prescription.getDoctorName())
            .appointmentId(prescription.getAppointmentId())
            .visitId(visitId)
            .status(prescription.getStatus() != null ? prescription.getStatus().name() : null)
            .createdAt(prescription.getCreatedAt());

    if (prescription.getItems() != null) {
      builder.items(
          prescription.getItems().stream().map(this::toItemDto).collect(Collectors.toList()));
    }

    if (prescription.getLabItems() != null) {
      builder.labItems(
          prescription.getLabItems().stream().map(this::toLabItemDto).collect(Collectors.toList()));
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

  private PrescriptionLabItemDTO toLabItemDto(PrescriptionLabItem item) {
    if (item == null) {
      return null;
    }
    return PrescriptionLabItemDTO.builder()
        .id(item.getId())
        .testId(item.getTestId())
        .testName(item.getTestName())
        .notes(item.getNotes())
        .status(item.getStatus() != null ? item.getStatus().name() : null)
        .build();
  }
}
