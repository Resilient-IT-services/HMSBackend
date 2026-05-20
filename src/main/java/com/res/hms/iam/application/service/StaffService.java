package com.res.hms.iam.application.service;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.iam.application.dto.DoctorStaffResponse;
import com.res.hms.iam.application.dto.NurseStaffResponse;
import com.res.hms.iam.domain.DoctorHospitalDetails;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.UserContractRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffService {

  private final UserContractRepository assignmentRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  @Transactional(readOnly = true)
  public DoctorStaffResponse getDoctor(UUID doctorId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    UserContract assignment =
        assignmentRepository
            .findByDoctorIdAndHospitalId(doctorId, hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Doctor not found", HttpStatus.NOT_FOUND));
    return mapToDoctorResponse(assignment);
  }

  @Transactional(readOnly = true)
  public NurseStaffResponse getNurse(UUID nurseId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    UserContract assignment =
        assignmentRepository
            .findByNurseIdAndHospitalId(nurseId, hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Nurse not found", HttpStatus.NOT_FOUND));
    return mapToNurseResponse(assignment);
  }

  @Transactional(readOnly = true)
  public PageResponse<DoctorStaffResponse> getDoctors(
      String name,
      String department,
      String specialization,
      BigDecimal minFee,
      BigDecimal maxFee,
      int page,
      int size) {

    UUID hospitalId = hospitalContext.requireHospitalId();
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Specification<UserContract> spec =
        (root, query, cb) -> {
          root.fetch("user", JoinType.INNER).fetch("doctorProfile", JoinType.LEFT);
          root.fetch("doctorDetails", JoinType.LEFT);

          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
          predicates.add(cb.equal(root.get("role").get("code"), RoleCode.DOCTOR));
          predicates.add(cb.equal(root.get("active"), true));
          predicates.add(cb.equal(root.get("user").get("active"), true));

          if (name != null && !name.isBlank()) {
            predicates.add(
                cb.like(
                    cb.lower(root.get("user").get("fullName")), "%" + name.toLowerCase() + "%"));
          }
          if (department != null && !department.isBlank()) {
            Join<UserContract, com.res.hms.hospital.domain.Department> deptJoin = root.join("departments");
            predicates.add(
                cb.like(
                    cb.lower(deptJoin.get("name")), "%" + department.toLowerCase() + "%"));
          }
          if (specialization != null && !specialization.isBlank()) {
            Join<UserContract, UserAccount> userJoin = root.join("user");
            Join<UserAccount, DoctorProfile> doctorJoin = userJoin.join("doctorProfile");
            Join<DoctorProfile, com.res.hms.iam.domain.Specialization> specJoin = doctorJoin.join("specializations");
            predicates.add(
                cb.like(
                    cb.lower(specJoin.get("name")),
                    "%" + specialization.toLowerCase() + "%"));
          }
          if (minFee != null) {
            Join<UserContract, DoctorHospitalDetails> detailsJoin =
                root.join("doctorDetails");
            predicates.add(cb.greaterThanOrEqualTo(detailsJoin.get("consultationFee"), minFee));
          }
          if (maxFee != null) {
            Join<UserContract, DoctorHospitalDetails> detailsJoin =
                root.join("doctorDetails");
            predicates.add(cb.lessThanOrEqualTo(detailsJoin.get("consultationFee"), maxFee));
          }

          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Page<UserContract> assignments = assignmentRepository.findAll(spec, pageable);
    return PageResponse.from(assignments.map(this::mapToDoctorResponse));
  }

  @Transactional(readOnly = true)
  public PageResponse<NurseStaffResponse> getNurses(
      String name, String department, int page, int size) {

    UUID hospitalId = hospitalContext.requireHospitalId();
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Specification<UserContract> spec =
        (root, query, cb) -> {
          root.fetch("user", JoinType.INNER).fetch("nurseProfile", JoinType.LEFT);
          root.fetch("nurseDetails", JoinType.LEFT);

          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
          predicates.add(cb.equal(root.get("role").get("code"), RoleCode.NURSE));
          predicates.add(cb.equal(root.get("active"), true));
          predicates.add(cb.equal(root.get("user").get("active"), true));

          if (name != null && !name.isBlank()) {
            predicates.add(
                cb.like(
                    cb.lower(root.get("user").get("fullName")), "%" + name.toLowerCase() + "%"));
          }
          if (department != null && !department.isBlank()) {
             Join<UserContract, com.res.hms.hospital.domain.Department> deptJoin = root.join("departments");
             predicates.add(
                cb.like(
                    cb.lower(deptJoin.get("name")), "%" + department.toLowerCase() + "%"));
          }

          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Page<UserContract> assignments = assignmentRepository.findAll(spec, pageable);
    return PageResponse.from(assignments.map(this::mapToNurseResponse));
  }

  @Transactional(readOnly = true)
  public List<com.res.hms.iam.application.dto.DoctorSelectionDTO> getDoctorSelection() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return assignmentRepository.findByHospitalIdAndRoleCodeAndActive(hospitalId, RoleCode.DOCTOR, true)
        .stream()
        .map(a -> {
          UserAccount u = a.getUser();
          DoctorProfile d = u.getDoctorProfile();
          String spec = (d != null && d.getSpecializations() != null) 
              ? d.getSpecializations().stream()
                  .map(com.res.hms.iam.domain.Specialization::getName)
                  .collect(Collectors.joining(", "))
              : "";
          return com.res.hms.iam.application.dto.DoctorSelectionDTO.builder()
              .id(d != null ? d.getId() : null)
              .fullName(u.getFullName())
              .specialization(spec)
              .build();
        })
        .filter(dto -> dto.getId() != null)
        .collect(Collectors.toList());
  }

  private DoctorStaffResponse mapToDoctorResponse(UserContract a) {
    UserAccount u = a.getUser();
    DoctorProfile d = u.getDoctorProfile();
    DoctorHospitalDetails det = a.getDoctorDetails();

    return DoctorStaffResponse.builder()
        .userId(u.getId())
        .doctorId(d != null ? d.getId() : null)
        .assignmentId(a.getId())
        .fullName(u.getFullName())
        .phone(u.getPhone())
        .gender(u.getGender())
        .departments(a.getDepartments().stream()
            .map(com.res.hms.hospital.domain.Department::getName)
            .collect(Collectors.toList()))
        .specializations(d != null ? d.getSpecializations().stream()
            .map(com.res.hms.iam.domain.Specialization::getName)
            .collect(Collectors.toList()) : null)
        .consultationFee(det != null ? det.getConsultationFee() : null)
        .build();
  }

  private NurseStaffResponse mapToNurseResponse(UserContract a) {
    UserAccount u = a.getUser();
    var n = u.getNurseProfile();
    var det = a.getNurseDetails();

    return NurseStaffResponse.builder()
        .userId(u.getId())
        .nurseId(n != null ? n.getId() : null)
        .assignmentId(a.getId())
        .fullName(u.getFullName())
        .phone(u.getPhone())
        .gender(u.getGender())
        .departments(a.getDepartments().stream()
            .map(com.res.hms.hospital.domain.Department::getName)
            .collect(Collectors.toList()))
        .certificationLevel(n != null ? n.getCertificationLevel() : null)
        .shiftType(det != null ? det.getShiftType() : null)
        .build();
  }
}
