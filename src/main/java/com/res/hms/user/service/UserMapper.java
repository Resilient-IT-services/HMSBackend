package com.res.hms.user.service;

import com.res.hms.domain.AdminHospitalDetails;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.DoctorHospitalDetails;
import com.res.hms.domain.LabTechProfile;
import com.res.hms.domain.Nurse;
import com.res.hms.domain.NurseHospitalDetails;
import com.res.hms.domain.PharmacistHospitalDetails;
import com.res.hms.domain.PharmacistProfile;
import com.res.hms.domain.ReceptionistHospitalDetails;
import com.res.hms.domain.ReceptionistProfile;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.user.dto.AdminHospitalDetailsDTO;
import com.res.hms.user.dto.DoctorHospitalDetailsDTO;
import com.res.hms.user.dto.HospitalMembershipResponse;
import com.res.hms.user.dto.NurseHospitalDetailsDTO;
import com.res.hms.user.dto.PharmacistHospitalDetailsDTO;
import com.res.hms.user.dto.ReceptionistHospitalDetailsDTO;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.dto.UserProfileResponse;
import com.res.hms.user.dto.UserSummaryDTO;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class UserMapper {

  @Autowired protected HospitalRepository hospitalRepository;
  @Autowired protected com.res.hms.common.tenant.HospitalContext hospitalContext;

  @Mapping(target = "memberships", expression = "java(mapMemberships(user))")
  public abstract UserSummaryDTO toSummaryDTO(UserAccount user);

  @Mapping(target = "memberships", expression = "java(mapMemberships(user))")
  @Mapping(target = "detailedMemberships", expression = "java(mapDetailedMemberships(user))")
  @Mapping(target = "hospitalId", expression = "java(hospitalContext.getHospitalIdOrNull())")
  @Mapping(target = "doctorInfo", source = "doctor")
  @Mapping(target = "nurseInfo", source = "nurse")
  @Mapping(target = "pharmacistInfo", source = "pharmacistProfile")
  @Mapping(target = "labTechInfo", source = "labTechProfile")
  @Mapping(target = "receptionistInfo", source = "receptionistProfile")
  public abstract UserFullProfileDTO toFullProfileDTO(UserAccount user);

  @Mapping(target = "userId", source = "id")
  @Mapping(target = "hospitalId", expression = "java(hospitalContext.getHospitalIdOrNull())")
  @Mapping(target = "hospitals", expression = "java(mapHospitalMemberships(user))")
  public abstract UserProfileResponse toUserProfileResponse(UserAccount user);

  protected List<HospitalMembershipResponse> mapHospitalMemberships(UserAccount user) {
    if (user.getAssignments() == null) {
      return Collections.emptyList();
    }

    return user.getAssignments().stream()
        .collect(Collectors.groupingBy(UserRoleAssignment::getHospitalId))
        .entrySet()
        .stream()
        .map(
            entry -> {
              UUID hospitalId = entry.getKey();
              var hospital =
                  hospitalId == null
                      ? null
                      : hospitalRepository.findById(hospitalId).orElse(null);

              Set<RoleCode> roles =
                  entry.getValue().stream()
                      .map(a -> a.getRole().getCode())
                      .collect(Collectors.toSet());

              String specialization =
                  user.getDoctor() != null ? user.getDoctor().getSpecialization() : null;
              String department = entry.getValue().getFirst().getDepartmentName();

              return new HospitalMembershipResponse(
                  hospitalId,
                  hospital != null ? hospital.getName() : "Global Access",
                  hospital != null ? hospital.getCode() : "GLOBAL",
                  roles,
                  specialization,
                  department);
            })
        .collect(Collectors.toList());
  }

  @Mapping(target = "assignmentId", source = "id")
  @Mapping(target = "roleCode", expression = "java(assignment.getRole().getCode().name())")
  @Mapping(target = "doctorDetails", source = "doctorDetails")
  @Mapping(target = "nurseDetails", source = "nurseDetails")
  @Mapping(target = "adminDetails", source = "adminDetails")
  @Mapping(target = "pharmacistDetails", source = "pharmacistDetails")
  @Mapping(target = "receptionistDetails", source = "receptionistDetails")
  public abstract UserFullProfileDTO.DetailedRoleAssignmentDTO toDetailedRoleAssignmentDTO(
      UserRoleAssignment assignment);

  public abstract UserFullProfileDTO.DoctorInfo toDoctorInfo(Doctor doctor);

  public abstract UserFullProfileDTO.NurseInfo toNurseInfo(Nurse nurse);

  public abstract UserFullProfileDTO.PharmacistInfo toPharmacistInfo(PharmacistProfile profile);

  public abstract UserFullProfileDTO.LabTechInfo toLabTechInfo(LabTechProfile profile);

  public abstract UserFullProfileDTO.ReceptionistInfo toReceptionistInfo(
      ReceptionistProfile profile);

  public abstract DoctorHospitalDetailsDTO toDoctorHospitalDetailsDTO(
      DoctorHospitalDetails details);

  public abstract NurseHospitalDetailsDTO toNurseHospitalDetailsDTO(NurseHospitalDetails details);

  public abstract AdminHospitalDetailsDTO toAdminHospitalDetailsDTO(AdminHospitalDetails details);

  public abstract PharmacistHospitalDetailsDTO toPharmacistHospitalDetailsDTO(
      PharmacistHospitalDetails details);

  public abstract ReceptionistHospitalDetailsDTO toReceptionistHospitalDetailsDTO(
      ReceptionistHospitalDetails details);

  protected List<UserSummaryDTO.HospitalMembershipDTO> mapMemberships(UserAccount user) {
    if (user.getAssignments() == null) {
      return Collections.emptyList();
    }

    return user.getAssignments().stream()
        .collect(Collectors.groupingBy(UserRoleAssignment::getHospitalId))
        .entrySet()
        .stream()
        .map(
            e -> {
              UUID hospitalId = e.getKey();
              List<UserRoleAssignment> hospitalAssignments = e.getValue();
              
              List<String> roles = hospitalAssignments.stream()
                  .map(a -> a.getRole().getCode().name())
                  .toList();
                  
              String specialization = user.getDoctor() != null ? user.getDoctor().getSpecialization() : null;
              String departmentName = hospitalAssignments.stream()
                  .map(UserRoleAssignment::getDepartmentName)
                  .filter(d -> d != null && !d.isBlank())
                  .findFirst()
                  .orElse(null);

              return new UserSummaryDTO.HospitalMembershipDTO(
                  hospitalId, roles, specialization, departmentName);
            })
        .collect(Collectors.toList());
  }

  protected List<UserFullProfileDTO.DetailedHospitalMembershipDTO> mapDetailedMemberships(
      UserAccount user) {
    if (user.getAssignments() == null) {
      return Collections.emptyList();
    }

    return user.getAssignments().stream()
        .collect(Collectors.groupingBy(UserRoleAssignment::getHospitalId))
        .entrySet()
        .stream()
        .map(
            entry -> {
              UUID hospitalId = entry.getKey();
              var hospital =
                  hospitalId == null
                      ? null
                      : hospitalRepository.findById(hospitalId).orElse(null);

              String hospitalName = hospital != null ? hospital.getName() : "Global Access";
              String hospitalCode = hospital != null ? hospital.getCode() : "GLOBAL";

              List<UserFullProfileDTO.DetailedRoleAssignmentDTO> roleAssignments =
                  entry.getValue().stream().map(this::toDetailedRoleAssignmentDTO).toList();

              return UserFullProfileDTO.DetailedHospitalMembershipDTO.builder()
                  .hospitalId(hospitalId)
                  .hospitalName(hospitalName)
                  .hospitalCode(hospitalCode)
                  .roleAssignments(roleAssignments)
                  .build();
            })
        .collect(Collectors.toList());
  }
}
