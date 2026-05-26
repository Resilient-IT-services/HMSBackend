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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientService {

  private final PatientRepository patientRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  private Specification<Patient> getFilterSpec(UUID hospitalId, String search) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
      predicates.add(cb.equal(root.get("deleted"), false));

      if (search != null && !search.trim().isEmpty()) {
        String pattern = "%" + search.trim().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("fullName")), pattern),
                cb.like(cb.lower(root.get("patientCode")), pattern),
                cb.like(root.get("phone"), pattern)));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  public PageResponse<PatientDTO> list(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Specification<Patient> spec = getFilterSpec(hospitalId, search);
    Page<Patient> results = patientRepository.findAll(spec, pageable);
    return PageResponse.from(results.map(this::toDto));
  }

  public PageResponse<PatientSummaryDTO> listSummary(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Specification<Patient> spec = getFilterSpec(hospitalId, search);
    Page<Patient> results = patientRepository.findAll(spec, pageable);
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
