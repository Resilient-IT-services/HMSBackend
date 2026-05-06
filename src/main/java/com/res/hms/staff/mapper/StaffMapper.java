package com.res.hms.staff.mapper;

import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.staff.dto.DoctorStaffResponse;
import com.res.hms.staff.dto.NurseStaffResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StaffMapper {

  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "doctorId", source = "user.doctorProfile.id")
  @Mapping(target = "assignmentId", source = "id")
  @Mapping(target = "fullName", source = "user.fullName")
  @Mapping(target = "phone", source = "user.phone")
  @Mapping(target = "gender", source = "user.gender")
  @Mapping(target = "specialization", source = "user.doctorProfile.specialization")
  @Mapping(target = "consultationFee", source = "doctorDetails.consultationFee")
  DoctorStaffResponse toDoctorResponse(UserRoleAssignment assignment);

  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "nurseId", source = "user.nurseProfile.id")
  @Mapping(target = "assignmentId", source = "id")
  @Mapping(target = "fullName", source = "user.fullName")
  @Mapping(target = "phone", source = "user.phone")
  @Mapping(target = "gender", source = "user.gender")
  @Mapping(target = "certificationLevel", source = "user.nurseProfile.certificationLevel")
  @Mapping(target = "shiftType", source = "nurseDetails.shiftType")
  NurseStaffResponse toNurseResponse(UserRoleAssignment assignment);
}
