package com.res.hms.staff.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.DoctorHospitalDetails;
import com.res.hms.domain.DoctorProfile;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.user.repository.UserRoleAssignmentRepository;
import com.res.hms.staff.dto.DoctorStaffResponse;
import com.res.hms.staff.dto.NurseStaffResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.staff.mapper.StaffMapper;

@Service
@RequiredArgsConstructor
public class StaffService {

  private final UserRoleAssignmentRepository assignmentRepository;
  private final HospitalContext hospitalContext;
  private final StaffMapper staffMapper;

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

    Specification<UserRoleAssignment> spec =
        (root, query, cb) -> {
          root.fetch("user", JoinType.INNER).fetch("doctorProfile", JoinType.LEFT);
          root.fetch("doctorDetails", JoinType.LEFT);

          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
          predicates.add(cb.equal(root.get("role").get("code"), RoleCode.DOCTOR));
          predicates.add(cb.equal(root.get("active"), true));

          if (name != null && !name.isBlank()) {
            predicates.add(
                cb.like(
                    cb.lower(root.get("user").get("fullName")), "%" + name.toLowerCase() + "%"));
          }
          if (department != null && !department.isBlank()) {
            predicates.add(
                cb.like(
                    cb.lower(root.get("departmentName")), "%" + department.toLowerCase() + "%"));
          }
          if (specialization != null && !specialization.isBlank()) {
            Join<UserRoleAssignment, UserAccount> userJoin = root.join("user");
            Join<UserAccount, DoctorProfile> doctorJoin = userJoin.join("doctorProfile");
            predicates.add(
                cb.like(
                    cb.lower(doctorJoin.get("specialization")),
                    "%" + specialization.toLowerCase() + "%"));
          }
          if (minFee != null) {
            Join<UserRoleAssignment, DoctorHospitalDetails> detailsJoin =
                root.join("doctorDetails");
            predicates.add(cb.greaterThanOrEqualTo(detailsJoin.get("consultationFee"), minFee));
          }
          if (maxFee != null) {
            Join<UserRoleAssignment, DoctorHospitalDetails> detailsJoin =
                root.join("doctorDetails");
            predicates.add(cb.lessThanOrEqualTo(detailsJoin.get("consultationFee"), maxFee));
          }

          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Page<UserRoleAssignment> assignments = assignmentRepository.findAll(spec, pageable);
    return PageResponse.from(assignments.map(staffMapper::toDoctorResponse));
  }

  @Transactional(readOnly = true)
  public PageResponse<NurseStaffResponse> getNurses(
      String name, String department, int page, int size) {

    UUID hospitalId = hospitalContext.requireHospitalId();
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Specification<UserRoleAssignment> spec =
        (root, query, cb) -> {
          root.fetch("user", JoinType.INNER).fetch("nurseProfile", JoinType.LEFT);
          root.fetch("nurseDetails", JoinType.LEFT);

          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
          predicates.add(cb.equal(root.get("role").get("code"), RoleCode.NURSE));
          predicates.add(cb.equal(root.get("active"), true));

          if (name != null && !name.isBlank()) {
            predicates.add(
                cb.like(
                    cb.lower(root.get("user").get("fullName")), "%" + name.toLowerCase() + "%"));
          }
          if (department != null && !department.isBlank()) {
            predicates.add(
                cb.like(
                    cb.lower(root.get("departmentName")), "%" + department.toLowerCase() + "%"));
          }

          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Page<UserRoleAssignment> assignments = assignmentRepository.findAll(spec, pageable);
    return PageResponse.from(assignments.map(staffMapper::toNurseResponse));
  }
}
