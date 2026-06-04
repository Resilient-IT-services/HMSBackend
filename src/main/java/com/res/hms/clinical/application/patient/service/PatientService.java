package com.res.hms.clinical.application.patient.service;

import com.res.hms.clinical.application.patient.dto.PatientDTO;
import com.res.hms.clinical.application.patient.dto.PatientDashboardStatsDTO;
import com.res.hms.clinical.application.patient.dto.PatientSummaryDTO;
import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientService {

  private final PatientRepository patientRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  @Transactional(readOnly = true)
  public PageResponse<PatientDTO> list(
      org.springframework.data.domain.Pageable pageable,
      String search,
      java.util.Map<String, String> filters) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    com.res.hms.core.util.SpecificationBuilder<Patient> builder =
        new com.res.hms.core.util.SpecificationBuilder<>(
            filters, search, java.util.List.of("fullName", "patientCode", "phone"));

    Specification<Patient> spec = builder.build();
    Specification<Patient> fixedSpec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
          predicates.add(cb.equal(root.get("deleted"), false));
          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Page<Patient> results =
        patientRepository.findAll(Specification.where(spec).and(fixedSpec), pageable);
    return PageResponse.from(results.map(this::toDto));
  }

  @Transactional(readOnly = true)
  public PageResponse<PatientSummaryDTO> listSummary(
      org.springframework.data.domain.Pageable pageable,
      String search,
      java.util.Map<String, String> filters) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    com.res.hms.core.util.SpecificationBuilder<Patient> builder =
        new com.res.hms.core.util.SpecificationBuilder<>(
            filters, search, java.util.List.of("fullName", "patientCode", "phone"));

    Specification<Patient> spec = builder.build();
    Specification<Patient> fixedSpec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
          predicates.add(cb.equal(root.get("deleted"), false));
          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Page<Patient> results =
        patientRepository.findAll(Specification.where(spec).and(fixedSpec), pageable);
    return PageResponse.from(results.map(this::toSummaryDto));
  }

  public PatientDTO get(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return toDto(requirePatient(id, hospitalId));
  }

  public PatientSummaryDTO getSummary(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return toSummaryDto(requirePatient(id, hospitalId));
  }

  public Patient requirePatient(UUID id, UUID hospitalId) {
    return resourceLookup.findRequired(
        id, hospitalId, patientRepository::findByIdAndHospitalId, "Patient");
  }

  public PatientDTO toDto(Patient patient) {
    return PatientDTO.builder()
        .id(patient.getId())
        .fullName(patient.getFullName())
        .patientCode(patient.getPatientCode())
        .email(patient.getEmail())
        .medicalHistory(patient.getMedicalHistory())
        .gender(patient.getGender())
        .dob(patient.getDob())
        .phone(patient.getPhone())
        .bloodGroup(patient.getBloodGroup())
        .address(patient.getAddress())
        .build();
  }

  public PatientSummaryDTO toSummaryDto(Patient patient) {
    return PatientSummaryDTO.builder()
        .id(patient.getId())
        .fullName(patient.getFullName())
        .patientCode(patient.getPatientCode())
        .gender(patient.getGender())
        .dob(patient.getDob())
        .phone(patient.getPhone())
        .build();
  }

  public PatientDashboardStatsDTO getDashboardStats() {

    long totalRecords = patientRepository.count();

    LocalDate today = LocalDate.now();

    long newToday =
        patientRepository.countByCreatedAtBetween(
            today.atStartOfDay(), today.plusDays(1).atStartOfDay());

    long inPatient = 0;

    return new PatientDashboardStatsDTO(totalRecords, newToday, inPatient);
  }
}
