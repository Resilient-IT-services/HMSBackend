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
import com.res.hms.repository.DoctorHospitalDetailsRepository;
import com.res.hms.user.dto.DetailedAssignmentDTO;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.dto.UserSummaryDTO;
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
  private final NurseRepository nurseRepository;
  private final ReceptionistProfileRepository receptionistProfileRepository;

  public UserSummaryDTO toSummaryDTO(UserAccount user) {
    Map<UUID, List<String>> rolesByHospital = new HashMap<>();
    if (user.getAssignments() != null) {
      for (UserRoleAssignment assignment : user.getAssignments()) {
        rolesByHospital
            .computeIfAbsent(assignment.getHospitalId(), k -> new ArrayList<>())
            .add(assignment.getRole().getCode().name());
      }
    }

    List<UserSummaryDTO.HospitalMembershipDTO> memberships = rolesByHospital.entrySet().stream()
        .map(e -> new UserSummaryDTO.HospitalMembershipDTO(e.getKey(), e.getValue()))
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

    List<DetailedAssignmentDTO> history = user.getAssignments().stream()
        .map(a -> {
            DetailedAssignmentDTO.DetailedAssignmentDTOBuilder builder = DetailedAssignmentDTO.builder()
                .id(a.getId())
                .hospitalId(a.getHospitalId())
                .roleCode(a.getRole().getCode().name())
                .departmentName(a.getDepartmentName())
                .active(a.isActive())
                .createdAt(a.getCreatedAt());
            
            if (a.getHospitalId() != null) {
                hospitalRepository.findById(a.getHospitalId())
                    .ifPresent(h -> builder.hospitalName(h.getName()));
                
                doctorHospitalDetailsRepository.findById(a.getId())
                    .ifPresent(d -> {
                        builder.consultationFee(d.getConsultationFee());
                        builder.followUpDays(d.getFollowUpDays());
                    });
            }
            return builder.build();
        })
        .collect(Collectors.toList());

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
        .assignmentHistory(history)
        .build();
  }
}
