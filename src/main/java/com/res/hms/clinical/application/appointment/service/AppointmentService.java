package com.res.hms.clinical.application.appointment.service;

import com.res.hms.audit.domain.DomainEntity;
import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.AppointmentSummaryDTO;
import com.res.hms.clinical.application.appointment.dto.UpdateAppointmentStatusRequest;
import com.res.hms.clinical.application.prescription.repository.PrescriptionRepository;
import com.res.hms.clinical.application.prescription.service.PrescriptionService;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.audit.Auditable;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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
public class AppointmentService {

  private final AppointmentRepository appointmentRepository;
  private final PrescriptionRepository prescriptionRepository;
  private final PrescriptionService prescriptionService;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;
  private final com.res.hms.hospital.infrastructure.repository.HospitalRepository
      hospitalRepository;
  private final EntityManager entityManager;

  @Transactional(readOnly = true)
  public AppointmentDTO getById(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return appointmentRepository
        .findByIdAndHospitalId(id, hospitalId)
        .map(this::toDto)
        .orElseThrow(
            () -> new ApiException("NOT_FOUND", "Appointment not found", HttpStatus.NOT_FOUND));
  }

  private Specification<Appointment> getFilterSpec(
      UUID hospitalId,
      String search,
      java.time.Instant from,
      java.time.Instant to,
      UUID patientId,
      UUID doctorId,
      com.res.hms.clinical.domain.AppointmentStatus status,
      String token,
      String patientName,
      String doctor) {

    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
      predicates.add(cb.equal(root.get("deleted"), false));

      if (patientId != null) {
        predicates.add(cb.equal(root.get("patientId"), patientId));
      }
      if (doctorId != null) {
        predicates.add(cb.equal(root.get("doctorId"), doctorId));
      }
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      if (search != null && !search.isBlank()) {
        String pattern = "%" + search.toLowerCase(Locale.ROOT).trim() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("doctorName")), pattern),
                cb.like(cb.lower(root.get("patientName")), pattern)));
      }
      if (token != null && !token.isBlank()) {
        predicates.add(cb.like(root.get("tokenNumber").as(String.class), "%" + token.trim() + "%"));
      }
      if (patientName != null && !patientName.isBlank()) {
        predicates.add(
            cb.like(
                cb.lower(root.get("patientName")),
                "%" + patientName.toLowerCase(Locale.ROOT).trim() + "%"));
      }
      if (doctor != null && !doctor.isBlank()) {
        predicates.add(
            cb.like(
                cb.lower(root.get("doctorName")),
                "%" + doctor.toLowerCase(Locale.ROOT).trim() + "%"));
      }
      if (from != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("appointmentTime"), from));
      }
      if (to != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("appointmentTime"), to));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  @Transactional(readOnly = true)
  public PageResponse<AppointmentDTO> list(
      int page,
      int size,
      String search,
      java.time.Instant from,
      java.time.Instant to,
      UUID patientId,
      UUID doctorId,
      com.res.hms.clinical.domain.AppointmentStatus status) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Pageable pageable = PageRequest.of(page, size, Sort.by("appointmentTime").descending());

    Specification<Appointment> spec =
        getFilterSpec(hospitalId, search, from, to, patientId, doctorId, status, null, null, null);

    Specification<Appointment> fetchSpec =
        (root, query, cb) -> {
          if (query.getResultType() != Long.class && query.getResultType() != long.class) {
            root.fetch("patient", JoinType.LEFT);
            root.fetch("doctor", JoinType.LEFT);
            root.fetch("vitals", JoinType.LEFT);
          }
          return null;
        };

    Page<Appointment> assignments =
        appointmentRepository.findAll(Specification.where(spec).and(fetchSpec), pageable);
    return PageResponse.from(assignments.map(this::toDto));
  }

  @Transactional(readOnly = true)
  public com.res.hms.clinical.application.appointment.dto.AppointmentStatsDTO getStats(
      String search,
      java.time.Instant from,
      java.time.Instant to,
      UUID patientId,
      UUID doctorId,
      com.res.hms.clinical.domain.AppointmentStatus status) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
    Root<Appointment> root = query.from(Appointment.class);

    Specification<Appointment> spec =
        getFilterSpec(hospitalId, search, from, to, patientId, doctorId, status, null, null, null);
    Predicate predicate = spec.toPredicate(root, query, cb);
    if (predicate != null) {
      query.where(predicate);
    }
    query.multiselect(root.get("status"), cb.count(root));
    query.groupBy(root.get("status"));
    List<Object[]> results = entityManager.createQuery(query).getResultList();

    java.util.Map<com.res.hms.clinical.domain.AppointmentStatus, Long> byStatus =
        new java.util.EnumMap<>(com.res.hms.clinical.domain.AppointmentStatus.class);
    long total = 0;

    for (Object[] result : results) {
      com.res.hms.clinical.domain.AppointmentStatus s =
          (com.res.hms.clinical.domain.AppointmentStatus) result[0];
      Long count = (Long) result[1];
      byStatus.put(s, count);
      total += count;
    }

    // Upcoming count logic (reuses the same filter but only looks at future appointments)
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    Root<Appointment> countRoot = countQuery.from(Appointment.class);
    Predicate baseCountPredicate =
        getFilterSpec(
                hospitalId, search, from, null, patientId, doctorId, status, null, null, null)
            .toPredicate(countRoot, countQuery, cb);

    java.time.Instant now = java.time.Instant.now();
    Predicate futurePredicate = cb.greaterThan(countRoot.get("appointmentTime"), now);

    if (baseCountPredicate != null) {
      countQuery.where(cb.and(baseCountPredicate, futurePredicate));
    } else {
      countQuery.where(futurePredicate);
    }

    countQuery.select(cb.count(countRoot));
    long upcoming = entityManager.createQuery(countQuery).getSingleResult();

    return com.res.hms.clinical.application.appointment.dto.AppointmentStatsDTO.builder()
        .total(total)
        .upcoming(upcoming)
        .byStatus(byStatus)
        .build();
  }

  @Transactional(readOnly = true)
  public PageResponse<AppointmentSummaryDTO> listSummary(
      int page,
      int size,
      String search,
      java.time.Instant from,
      java.time.Instant to,
      UUID patientId,
      UUID doctorId,
      com.res.hms.clinical.domain.AppointmentStatus status,
      String token,
      String patientName,
      String doctor,
      String sort) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    Sort parsedSort = Sort.by("appointmentTime").descending();
    if (sort != null && !sort.isBlank()) {
      String[] parts = sort.split(",");
      String property = parts[0].trim();
      Sort.Direction direction = Sort.Direction.ASC;
      if (parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")) {
        direction = Sort.Direction.DESC;
      }
      parsedSort = Sort.by(direction, property);
    }

    Pageable pageable = PageRequest.of(page, size, parsedSort);

    Specification<Appointment> spec =
        getFilterSpec(
            hospitalId, search, from, to, patientId, doctorId, status, token, patientName, doctor);

    Specification<Appointment> fetchSpec =
        (root, query, cb) -> {
          if (query.getResultType() != Long.class && query.getResultType() != long.class) {
            root.fetch("patient", JoinType.LEFT);
            root.fetch("doctor", JoinType.LEFT);
          }
          return null;
        };

    Page<Appointment> results =
        appointmentRepository.findAll(Specification.where(spec).and(fetchSpec), pageable);
    return PageResponse.from(results.map(this::toSummaryDto));
  }

  @Transactional
  @Auditable(entity = DomainEntity.APPOINTMENT, action = "UPDATE_STATUS")
  public AppointmentDTO updateStatus(UUID id, UpdateAppointmentStatusRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Appointment appointment =
        resourceLookup.findRequired(
            id, hospitalId, appointmentRepository::findByIdAndHospitalId, "Appointment");

    if (hospitalContext.isReceptionist()) {
      if (appointment.getStatus()
              == com.res.hms.clinical.domain.AppointmentStatus.CONSULTATION_STARTED
          || appointment.getStatus()
              == com.res.hms.clinical.domain.AppointmentStatus.PRESCRIPTION_FINALIZED
          || appointment.getStatus() == com.res.hms.clinical.domain.AppointmentStatus.COMPLETED) {
        throw new ApiException(
            "BUSINESS_RULE_VIOLATION",
            "Receptionists cannot change appointment status once consultation has started.",
            HttpStatus.BAD_REQUEST);
      }
    }

    appointment.setStatus(request.status());
    return toDto(appointmentRepository.save(appointment));
  }

  public AppointmentDTO toDto(Appointment appointment) {
    if (appointment == null) {
      return null;
    }

    AppointmentDTO.AppointmentDTOBuilder builder =
        AppointmentDTO.builder()
            .id(appointment.getId())
            .appointmentTime(appointment.getAppointmentTime().atOffset(java.time.ZoneOffset.UTC))
            .appointmentDate(appointment.getAppointmentDate())
            .appointmentSlotTime(appointment.getAppointmentSlotTime())
            .status(appointment.getStatus())
            .tokenNumber(appointment.getTokenNumber())
            .sessionPeriod(appointment.getSessionPeriod())
            .chiefComplaints(appointment.getChiefComplaints())
            .diagnosis(appointment.getDiagnosis())
            .clinicalNotes(appointment.getClinicalNotes())
            .icdCodes(appointment.getIcdCodes())
            .notes(appointment.getNotes())
            .vitals(toVitalsDto(appointment.getVitals()))
            .visitId(appointment.getVisitId());

    // Fetch prescription via appointmentId instead of direct entity link or shared ID
    prescriptionRepository
        .findByAppointmentId(appointment.getId())
        .ifPresent(p -> builder.prescription(prescriptionService.toDto(p)));

    if (appointment.getPatientId() != null) {
      builder
          .patientId(appointment.getPatientId())
          .patientName(appointment.getPatientName())
          .patientCode(
              appointment.getPatient() != null ? appointment.getPatient().getPatientCode() : null);
    }

    if (appointment.getDoctorId() != null) {
      builder.doctorId(appointment.getDoctorId()).doctorName(appointment.getDoctorName());
    }

    return builder.build();
  }

  public AppointmentSummaryDTO toSummaryDto(Appointment appointment) {
    if (appointment == null) {
      return null;
    }

    return AppointmentSummaryDTO.builder()
        .id(appointment.getId())
        .patientId(appointment.getPatientId())
        .patientName(appointment.getPatientName())
        .patientCode(
            appointment.getPatient() != null ? appointment.getPatient().getPatientCode() : null)
        .doctorId(appointment.getDoctorId())
        .doctorName(appointment.getDoctorName())
        .doctorSpecialization(
            appointment.getDoctor() != null
                    && appointment.getDoctor().getSpecializations() != null
                    && !appointment.getDoctor().getSpecializations().isEmpty()
                ? appointment.getDoctor().getSpecializations().stream()
                    .map(com.res.hms.iam.domain.Specialization::getName)
                    .collect(java.util.stream.Collectors.joining(", "))
                : null)
        .appointmentTime(appointment.getAppointmentTime().atOffset(java.time.ZoneOffset.UTC))
        .status(appointment.getStatus())
        .tokenNumber(appointment.getTokenNumber())
        .build();
  }

  private com.res.hms.clinical.application.appointment.dto.VitalsDTO toVitalsDto(
      com.res.hms.clinical.domain.Vitals vitals) {
    if (vitals == null) {
      return null;
    }
    return com.res.hms.clinical.application.appointment.dto.VitalsDTO.builder()
        .bloodPressure(vitals.getBloodPressure())
        .systolic(vitals.getSystolic())
        .diastolic(vitals.getDiastolic())
        .heartRate(vitals.getHeartRate())
        .oxygenLevel(vitals.getOxygenLevel())
        .temperature(vitals.getTemperature())
        .weight(vitals.getWeight())
        .build();
  }
}
