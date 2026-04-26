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
import com.res.hms.user.dto.UserDetailsDTO;
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

  @Mapping(target = "memberships", expression = "java(mapSummaryMemberships(user))")
  public abstract UserSummaryDTO toSummaryDTO(UserAccount user);

  @Mapping(target = "memberships", expression = "java(mapMemberships(user))")
  @Mapping(target = "doctorInfo", source = "doctor")
  @Mapping(target = "nurseInfo", source = "nurse")
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
              var hospital = first.getHospital();
              if (hospital == null && hospitalId != null) {
                hospital = hospitalRepository.findById(hospitalId).orElse(null);
              }

              Set<RoleCode> roles =
                  entry.getValue().stream()
                      .map(a -> a.getRole().getCode())
                      .collect(Collectors.toSet());

              String specialization =
                  user.getDoctor() != null ? user.getDoctor().getSpecialization() : null;
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
  @Mapping(target = "schedules", source = "schedules")
  @Mapping(
      target = "availabilityExceptions",
      expression = "java(mapAvailabilityExceptions(assignment))")
  public abstract UserDetailsDTO.RoleAssignmentDTO toRoleAssignmentDTO(
      UserRoleAssignment assignment);

  protected List<com.res.hms.doctor.dto.AvailabilityExceptionDTO> mapAvailabilityExceptions(
      UserRoleAssignment assignment) {
    if (assignment.getUser() == null || assignment.getUser().getDoctor() == null) {
      return null;
    }
    return assignment.getUser().getDoctor().getAvailabilityExceptions().stream()
        .filter(ex -> ex.getHospital().getId().equals(assignment.getHospitalId()))
        .map(this::toAvailabilityExceptionDTO)
        .toList();
  }

  public abstract UserDetailsDTO.DoctorInfo toDoctorInfo(Doctor doctor);

  public abstract UserDetailsDTO.NurseInfo toNurseInfo(Nurse nurse);

  public abstract UserDetailsDTO.PharmacistInfo toPharmacistInfo(PharmacistProfile profile);

  public abstract UserDetailsDTO.LabTechInfo toLabTechInfo(LabTechProfile profile);

  public abstract UserDetailsDTO.ReceptionistInfo toReceptionistInfo(ReceptionistProfile profile);

  public abstract com.res.hms.doctor.dto.ScheduleDTO toScheduleDTO(
      com.res.hms.domain.DoctorSchedule schedule);

  public abstract com.res.hms.doctor.dto.AvailabilityExceptionDTO toAvailabilityExceptionDTO(
      com.res.hms.domain.DoctorAvailabilityException unavailability);

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
                  user.getDoctor() != null ? user.getDoctor().getSpecialization() : null;
              String hospitalName =
                  first.getHospital() != null ? first.getHospital().getName() : "Global Access";

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
              var first = entry.getValue().getFirst();
              var hospital = first.getHospital();
              if (hospital == null && hospitalId != null) {
                hospital = hospitalRepository.findById(hospitalId).orElse(null);
              }

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
