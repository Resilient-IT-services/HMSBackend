package com.res.hms.iam.application.service;

import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.application.dto.AdminHospitalDetailsDTO;
import com.res.hms.iam.application.dto.DoctorHospitalDetailsDTO;
import com.res.hms.iam.application.dto.HospitalMembershipResponse;
import com.res.hms.iam.application.dto.NurseHospitalDetailsDTO;
import com.res.hms.iam.application.dto.PharmacistHospitalDetailsDTO;
import com.res.hms.iam.application.dto.ReceptionistHospitalDetailsDTO;
import com.res.hms.iam.application.dto.UserDetailsDTO;
import com.res.hms.iam.application.dto.UserSummaryDTO;
import com.res.hms.iam.domain.AdminHospitalDetails;
import com.res.hms.iam.domain.DoctorHospitalDetails;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.LabTechProfile;
import com.res.hms.iam.domain.NurseHospitalDetails;
import com.res.hms.iam.domain.NurseProfile;
import com.res.hms.iam.domain.PharmacistHospitalDetails;
import com.res.hms.iam.domain.PharmacistProfile;
import com.res.hms.iam.domain.ReceptionistHospitalDetails;
import com.res.hms.iam.domain.ReceptionistProfile;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserRoleAssignment;
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
  @Autowired protected com.res.hms.core.tenant.HospitalContext hospitalContext;
  @Mapping(target = "memberships", expression = "java(mapSummaryMemberships(user))")
  public abstract UserSummaryDTO toSummaryDTO(UserAccount user);
  @Mapping(target = "memberships", expression = "java(mapMemberships(user))")
  @Mapping(target = "doctorInfo", source = "doctorProfile")
  @Mapping(target = "nurseInfo", source = "nurseProfile")
  @Mapping(target = "pharmacistInfo", source = "pharmacistProfile")
  @Mapping(target = "labTechInfo", source = "labTechProfile")
  @Mapping(target = "receptionistInfo", source = "receptionistProfile")
  public abstract UserDetailsDTO toDetailsDTO(UserAccount user);

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
              UserRoleAssignment first = entry.getValue().getFirst();
              var hospital = hospitalId != null ? hospitalRepository.findById(hospitalId).orElse(null) : null;

              Set<RoleCode> roles =
                  entry.getValue().stream()
                      .map(a -> a.getRole().getCode())
                      .collect(Collectors.toSet());

              String specialization =
                  user.getDoctorProfile() != null
                      ? user.getDoctorProfile().getSpecialization()
                      : null;
              String department = first.getDepartmentName();

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
  public abstract UserDetailsDTO.RoleAssignmentDTO toRoleAssignmentDTO(
      UserRoleAssignment assignment);

  @Mapping(target = "id", source = "id")
  public abstract UserDetailsDTO.DoctorInfo toDoctorInfo(DoctorProfile profile);

  @Mapping(target = "id", source = "id")
  public abstract UserDetailsDTO.NurseInfo toNurseInfo(NurseProfile profile);

  public abstract UserDetailsDTO.PharmacistInfo toPharmacistInfo(PharmacistProfile profile);

  public abstract UserDetailsDTO.LabTechInfo toLabTechInfo(LabTechProfile profile);

  public abstract UserDetailsDTO.ReceptionistInfo toReceptionistInfo(ReceptionistProfile profile);

  public abstract com.res.hms.iam.application.dto.ScheduleDTO toScheduleDTO(
      com.res.hms.clinical.domain.DoctorSchedule schedule);

  public abstract com.res.hms.iam.application.dto.AvailabilityExceptionDTO
      toAvailabilityExceptionDTO(
          com.res.hms.clinical.domain.DoctorAvailabilityException unavailability);

  public abstract DoctorHospitalDetailsDTO toDoctorHospitalDetailsDTO(
      DoctorHospitalDetails details);

  public abstract NurseHospitalDetailsDTO toNurseHospitalDetailsDTO(NurseHospitalDetails details);

  public abstract AdminHospitalDetailsDTO toAdminHospitalDetailsDTO(AdminHospitalDetails details);

  public abstract PharmacistHospitalDetailsDTO toPharmacistHospitalDetailsDTO(
      PharmacistHospitalDetails details);

  public abstract ReceptionistHospitalDetailsDTO toReceptionistHospitalDetailsDTO(
      ReceptionistHospitalDetails details);

  protected List<UserSummaryDTO.HospitalMembershipDTO> mapSummaryMemberships(UserAccount user) {
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
              UserRoleAssignment first = hospitalAssignments.getFirst();

              List<String> roles =
                  hospitalAssignments.stream().map(a -> a.getRole().getCode().name()).toList();

              String specialization =
                  user.getDoctorProfile() != null
                      ? user.getDoctorProfile().getSpecialization()
                      : null;
              
              String hospitalName = "Global Access";
              if (hospitalId != null) {
                  hospitalName = hospitalRepository.findById(hospitalId)
                      .map(com.res.hms.hospital.domain.Hospital::getName)
                      .orElse("Global Access");
              }

              String departmentName = first.getDepartmentName();

              return new UserSummaryDTO.HospitalMembershipDTO(
                  hospitalId, hospitalName, roles, specialization, departmentName);
            })
        .collect(Collectors.toList());
  }

  protected List<UserDetailsDTO.HospitalMembershipDTO> mapMemberships(UserAccount user) {
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
              var hospital = hospitalId != null ? hospitalRepository.findById(hospitalId).orElse(null) : null;

              String hospitalName = hospital != null ? hospital.getName() : "Global Access";
              String hospitalCode = hospital != null ? hospital.getCode() : "GLOBAL";

              List<UserDetailsDTO.RoleAssignmentDTO> roleAssignments =
                  entry.getValue().stream().map(this::toRoleAssignmentDTO).toList();

              return UserDetailsDTO.HospitalMembershipDTO.builder()
                  .hospitalId(hospitalId)
                  .hospitalName(hospitalName)
                  .hospitalCode(hospitalCode)
                  .roleAssignments(roleAssignments)
                  .build();
            })
        .collect(Collectors.toList());
  }
}
