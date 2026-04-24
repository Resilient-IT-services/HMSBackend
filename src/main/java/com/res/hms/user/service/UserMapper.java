package com.res.hms.user.service;

import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.Nurse;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.ReceptionistProfileRepository;
import com.res.hms.repository.PharmacistProfileRepository;
import com.res.hms.repository.LabTechProfileRepository;
import com.res.hms.repository.DoctorHospitalDetailsRepository;
import com.res.hms.repository.AdminHospitalDetailsRepository;
import com.res.hms.repository.NurseHospitalDetailsRepository;
import com.res.hms.repository.PharmacistHospitalDetailsRepository;
import com.res.hms.repository.ReceptionistHospitalDetailsRepository;
import com.res.hms.user.dto.*;
import java.math.BigDecimal;
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
    Map<UUID, List<UserSummaryDTO.RoleAssignmentDTO>> groupedAssignments = new HashMap<>();
    
    if (user.getAssignments() != null) {
        for (UserRoleAssignment a : user.getAssignments()) {
            UserSummaryDTO.RoleAssignmentDTO roleDto = UserSummaryDTO.RoleAssignmentDTO.builder()
                    .assignmentId(a.getId())
                    .roleCode(a.getRole().getCode().name())
                    .departmentName(a.getDepartmentName())
                    .active(a.isActive())
                    .hospitalDetails(fetchHospitalDetails(a))
                    .build();
            
            groupedAssignments.computeIfAbsent(a.getHospitalId(), k -> new ArrayList<>()).add(roleDto);
        }
    }

    List<UserSummaryDTO.HospitalMembershipDTO> memberships = groupedAssignments.entrySet().stream()
        .map(e -> {
            String hospitalName = null;
            if (e.getKey() != null) {
                hospitalName = hospitalRepository.findById(e.getKey()).map(Hospital::getName).orElse("Unknown");
            } else {
                hospitalName = "Global Access";
            }
            return UserSummaryDTO.HospitalMembershipDTO.builder()
                    .hospitalId(e.getKey())
                    .hospitalName(hospitalName)
                    .roleAssignments(e.getValue())
                    .build();
        })
        .collect(Collectors.toList());

    return UserSummaryDTO.builder()
        .id(user.getId())
        .username(user.getUsername())
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
    
    UserFullProfileDTO.DoctorInfo doctorInfo = doctorRepository.findByUserId(user.getId())
        .map(d -> UserFullProfileDTO.DoctorInfo.builder()
            .specialization(d.getSpecialization())
            .education(d.getEducation())
            .bio(d.getBio())
            .licenseNumber(d.getLicenseNumber())
            .build())
        .orElse(null);

    UserFullProfileDTO.NurseInfo nurseInfo = nurseRepository.findByUserId(user.getId())
        .map(n -> UserFullProfileDTO.NurseInfo.builder()
            .department(n.getDepartment())
            .nursingLicenseNo(n.getNursingLicenseNo())
            .certificationLevel(n.getCertificationLevel())
            .build())
        .orElse(null);

    UserFullProfileDTO.PharmacistInfo pharmacistInfo = pharmacistProfileRepository.findById(user.getId())
        .map(p -> UserFullProfileDTO.PharmacistInfo.builder()
            .pciRegNo(p.getPciRegNo())
            .qualification(p.getQualification())
            .build())
        .orElse(null);

    UserFullProfileDTO.LabTechInfo labTechInfo = labTechProfileRepository.findById(user.getId())
        .map(l -> UserFullProfileDTO.LabTechInfo.builder()
            .specialty(l.getSpecialty())
            .licenseNo(l.getLicenseNo())
            .build())
        .orElse(null);

    UserFullProfileDTO.ReceptionistInfo receptionistInfo = receptionistProfileRepository.findById(user.getId())
        .map(r -> UserFullProfileDTO.ReceptionistInfo.builder()
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
        .memberships(summary.getMemberships())
        .doctorInfo(doctorInfo)
        .nurseInfo(nurseInfo)
        .pharmacistInfo(pharmacistInfo)
        .labTechInfo(labTechInfo)
        .receptionistInfo(receptionistInfo)
        .build();
  }

  private Object fetchHospitalDetails(UserRoleAssignment a) {
      RoleCode code = a.getRole().getCode();
      switch (code) {
          case DOCTOR -> {
              return doctorHospitalDetailsRepository.findById(a.getId())
                      .map(d -> new DoctorHospitalDetailsDTO(d.getConsultationFee(), d.getFollowUpDays()))
                      .orElse(null);
          }
          case ADMIN -> {
              return adminHospitalDetailsRepository.findById(a.getId())
                      .map(d -> new AdminHospitalDetailsDTO(d.getOfficeLocation(), d.isDepartmentHead()))
                      .orElse(null);
          }
          case NURSE -> {
              return nurseHospitalDetailsRepository.findById(a.getId())
                      .map(d -> new NurseHospitalDetailsDTO(d.getFloorLocation(), d.getShiftType()))
                      .orElse(null);
          }
          case PHARMACIST -> {
              return pharmacistHospitalDetailsRepository.findById(a.getId())
                      .map(d -> new PharmacistHospitalDetailsDTO(d.getCounterLocation(), d.getPharmacyWing()))
                      .orElse(null);
          }
          case RECEPTIONIST -> {
              return receptionistHospitalDetailsRepository.findById(a.getId())
                      .map(d -> new ReceptionistHospitalDetailsDTO(d.getDeskLocation(), d.getEmergencyContactNumber()))
                      .orElse(null);
          }
          default -> { return null; }
      }
  }
}
