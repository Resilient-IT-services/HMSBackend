package com.res.hms.user.service;

import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.AdminHospitalDetailsRepository;
import com.res.hms.repository.DoctorHospitalDetailsRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.LabTechProfileRepository;
import com.res.hms.repository.NurseHospitalDetailsRepository;
import com.res.hms.repository.PharmacistHospitalDetailsRepository;
import com.res.hms.repository.PharmacistProfileRepository;
import com.res.hms.repository.ReceptionistHospitalDetailsRepository;
import com.res.hms.repository.ReceptionistProfileRepository;
import com.res.hms.user.dto.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

  private final HospitalRepository hospitalRepository;
  private final DoctorRepository doctorRepository;
  private final DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  private final AdminHospitalDetailsRepository adminHospitalDetailsRepository;
  private final NurseHospitalDetailsRepository nurseHospitalDetailsRepository;
  private final PharmacistHospitalDetailsRepository pharmacistHospitalDetailsRepository;
  private final ReceptionistHospitalDetailsRepository receptionistHospitalDetailsRepository;
  private final NurseRepository nurseRepository;
  private final PharmacistProfileRepository pharmacistProfileRepository;
  private final LabTechProfileRepository labTechProfileRepository;
  private final ReceptionistProfileRepository receptionistProfileRepository;

  public UserSummaryDTO toSummaryDTO(UserAccount user) {
    Map<UUID, Set<RoleCode>> rolesByHospital = new HashMap<>();
    if (user.getAssignments() != null) {
      for (UserRoleAssignment assignment : user.getAssignments()) {
        rolesByHospital
            .computeIfAbsent(assignment.getHospitalId(), k -> new HashSet<>())
            .add(assignment.getRole().getCode());
      }
    }

    List<UserSummaryDTO.HospitalMembershipDTO> memberships =
        rolesByHospital.entrySet().stream()
            .map(
                e ->
                    new UserSummaryDTO.HospitalMembershipDTO(
                        e.getKey(), e.getValue().stream().map(Enum::name).toList()))
            .collect(Collectors.toList());

    return UserSummaryDTO.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .fullName(user.getFullName())
        .gender(user.getGender())
        .dob(user.getDob())
        .phone(user.getPhone())
        .active(user.isActive())
        .deleted(user.isDeleted())
        .superuser(user.isSuperuser())
        .createdAt(user.getCreatedAt())
        .memberships(memberships)
        .build();
  }

  public UserFullProfileDTO toFullProfileDTO(UserAccount user) {
    UserSummaryDTO summary = toSummaryDTO(user);

    // Group assignments for Detailed view
    Map<UUID, List<UserFullProfileDTO.DetailedRoleAssignmentDTO>> groupedDetailedAssignments =
        new HashMap<>();
    if (user.getAssignments() != null) {
      for (UserRoleAssignment a : user.getAssignments()) {
        UserFullProfileDTO.DetailedRoleAssignmentDTO roleDto =
            UserFullProfileDTO.DetailedRoleAssignmentDTO.builder()
                .assignmentId(a.getId())
                .roleCode(a.getRole().getCode().name())
                .departmentName(a.getDepartmentName())
                .active(a.isActive())
                .build();

        mapDetailedHospitalInfo(a, roleDto);

        groupedDetailedAssignments
            .computeIfAbsent(a.getHospitalId(), k -> new ArrayList<>())
            .add(roleDto);
      }
    }

    List<UserFullProfileDTO.DetailedHospitalMembershipDTO> detailedMemberships =
        groupedDetailedAssignments.entrySet().stream()
            .map(
                e -> {
                  String hospitalName = null;
                  if (e.getKey() != null) {
                    hospitalName =
                        hospitalRepository
                            .findById(e.getKey())
                            .map(Hospital::getName)
                            .orElse("Unknown");
                  } else {
                    hospitalName = "Global Access";
                  }
                  return UserFullProfileDTO.DetailedHospitalMembershipDTO.builder()
                      .hospitalId(e.getKey())
                      .hospitalName(hospitalName)
                      .roleAssignments(e.getValue())
                      .build();
                })
            .collect(Collectors.toList());

    UserFullProfileDTO.DoctorInfo doctorInfo =
        doctorRepository
            .findByUserId(user.getId())
            .map(
                d ->
                    UserFullProfileDTO.DoctorInfo.builder()
                        .specialization(d.getSpecialization())
                        .education(d.getEducation())
                        .bio(d.getBio())
                        .licenseNumber(d.getLicenseNumber())
                        .build())
            .orElse(null);

    UserFullProfileDTO.NurseInfo nurseInfo =
        nurseRepository
            .findByUserId(user.getId())
            .map(
                n ->
                    UserFullProfileDTO.NurseInfo.builder()
                        .department(n.getDepartment())
                        .nursingLicenseNo(n.getNursingLicenseNo())
                        .certificationLevel(n.getCertificationLevel())
                        .build())
            .orElse(null);

    UserFullProfileDTO.PharmacistInfo pharmacistInfo =
        pharmacistProfileRepository
            .findById(user.getId())
            .map(
                p ->
                    UserFullProfileDTO.PharmacistInfo.builder()
                        .pciRegNo(p.getPciRegNo())
                        .qualification(p.getQualification())
                        .build())
            .orElse(null);

    UserFullProfileDTO.LabTechInfo labTechInfo =
        labTechProfileRepository
            .findById(user.getId())
            .map(
                l ->
                    UserFullProfileDTO.LabTechInfo.builder()
                        .specialty(l.getSpecialty())
                        .licenseNo(l.getLicenseNo())
                        .build())
            .orElse(null);

    UserFullProfileDTO.ReceptionistInfo receptionistInfo =
        receptionistProfileRepository
            .findById(user.getId())
            .map(
                r ->
                    UserFullProfileDTO.ReceptionistInfo.builder()
                        .languagesSpoken(r.getLanguagesSpoken())
                        .insuranceSpecialist(r.isInsuranceSpecialist())
                        .build())
            .orElse(null);

    return UserFullProfileDTO.builder()
        .id(summary.getId())
        .username(summary.getUsername())
        .fullName(summary.getFullName())
        .gender(summary.getGender())
        .dob(summary.getDob())
        .phone(summary.getPhone())
        .active(summary.isActive())
        .deleted(summary.isDeleted())
        .superuser(summary.isSuperuser())
        .createdAt(summary.getCreatedAt())
        .memberships(summary.getMemberships()) // Lightweight memberships
        .detailedMemberships(detailedMemberships) // Heavyweight nested memberships
        .doctorInfo(doctorInfo)
        .nurseInfo(nurseInfo)
        .pharmacistInfo(pharmacistInfo)
        .labTechInfo(labTechInfo)
        .receptionistInfo(receptionistInfo)
        .build();
  }

  private void mapDetailedHospitalInfo(
      UserRoleAssignment a, UserFullProfileDTO.DetailedRoleAssignmentDTO roleDto) {
    RoleCode code = a.getRole().getCode();
    switch (code) {
      case DOCTOR ->
          doctorHospitalDetailsRepository
              .findById(a.getId())
              .ifPresent(
                  d ->
                      roleDto.setDoctorDetails(
                          DoctorHospitalDetailsDTO.builder()
                              .consultationFee(d.getConsultationFee())
                              .followUpDays(d.getFollowUpDays())
                              .commissionType(d.getCommissionType())
                              .commissionValue(d.getCommissionValue())
                              .visitingConsultant(d.isVisitingConsultant())
                              .updatedAt(d.getUpdatedAt())
                              .build()));
      case ADMIN ->
          adminHospitalDetailsRepository
              .findById(a.getId())
              .ifPresent(
                  d ->
                      roleDto.setAdminDetails(
                          AdminHospitalDetailsDTO.builder()
                              .officeLocation(d.getOfficeLocation())
                              .departmentHead(d.isDepartmentHead())
                              .updatedAt(d.getUpdatedAt())
                              .build()));
      case NURSE ->
          nurseHospitalDetailsRepository
              .findById(a.getId())
              .ifPresent(
                  d ->
                      roleDto.setNurseDetails(
                          NurseHospitalDetailsDTO.builder()
                              .floorLocation(d.getFloorLocation())
                              .shiftType(d.getShiftType())
                              .updatedAt(d.getUpdatedAt())
                              .build()));
      case PHARMACIST ->
          pharmacistHospitalDetailsRepository
              .findById(a.getId())
              .ifPresent(
                  d ->
                      roleDto.setPharmacistDetails(
                          PharmacistHospitalDetailsDTO.builder()
                              .counterLocation(d.getCounterLocation())
                              .pharmacyWing(d.getPharmacyWing())
                              .updatedAt(d.getUpdatedAt())
                              .build()));
      case RECEPTIONIST ->
          receptionistHospitalDetailsRepository
              .findById(a.getId())
              .ifPresent(
                  d ->
                      roleDto.setReceptionistDetails(
                          ReceptionistHospitalDetailsDTO.builder()
                              .deskLocation(d.getDeskLocation())
                              .emergencyContactNumber(d.getEmergencyContactNumber())
                              .updatedAt(d.getUpdatedAt())
                              .build()));
    }
  }
}
