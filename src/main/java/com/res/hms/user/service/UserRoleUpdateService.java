package com.res.hms.user.service;

import com.res.hms.doctor.service.DoctorService;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.nurse.service.NurseService;
import com.res.hms.user.dto.UserDetailsDTO;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRoleUpdateService {

  private final DoctorService doctorService;
  private final NurseService nurseService;
  private final PharmacistProfileService pharmacistProfileService;
  private final LabTechProfileService labTechProfileService;
  private final ReceptionistProfileService receptionistProfileService;
  private final AdminProfileService adminProfileService;

  @Transactional
  public void handleProfileUpdate(
      UserAccount user, UserDetailsDTO request, boolean isSuper, UUID activeId) {
    Set<RoleCode> allRoles = user.getRoleCodes();

    doctorService.updateProfile(user, request.getDoctorInfo());
    nurseService.updateProfile(user, request.getNurseInfo());
    pharmacistProfileService.updateProfile(user, request.getPharmacistInfo(), allRoles);
    labTechProfileService.updateProfile(user, request.getLabTechInfo());
    receptionistProfileService.updateProfile(user, request.getReceptionistInfo(), allRoles);

    if (request.getMemberships() != null) {
      for (UserDetailsDTO.HospitalMembershipDTO m : request.getMemberships()) {
        UUID hId = m.getHospitalId();
        if (!isSuper && activeId != null) {
          hId = activeId;
        }
        if (hId != null && m.getRoleAssignments() != null) {
          for (UserDetailsDTO.RoleAssignmentDTO ra : m.getRoleAssignments()) {
            updateRoleSpecificHospitalDetails(user, hId, ra);
          }
        }
      }
    }
  }

  private void updateRoleSpecificHospitalDetails(
      UserAccount user, UUID hospitalId, UserDetailsDTO.RoleAssignmentDTO raDto) {
    RoleCode code = RoleCode.valueOf(raDto.getRoleCode());
    findAssignment(user, hospitalId, code)
        .ifPresent(
            a -> {
              switch (code) {
                case DOCTOR ->
                    doctorService.updateHospitalDetails(
                        a,
                        raDto.getDoctorDetails(),
                        raDto.getSchedules(),
                        raDto.getAvailabilityExceptions());
                case NURSE -> nurseService.updateHospitalDetails(a, raDto.getNurseDetails());
                case PHARMACIST ->
                    pharmacistProfileService.updateHospitalDetails(a, raDto.getPharmacistDetails());
                case RECEPTIONIST ->
                    receptionistProfileService.updateHospitalDetails(
                        a, raDto.getReceptionistDetails());
                case ADMIN -> adminProfileService.updateHospitalDetails(a, raDto.getAdminDetails());
                default -> { }
                }

            });
  }

  private java.util.Optional<UserRoleAssignment> findAssignment(
      UserAccount user, UUID hospitalId, RoleCode code) {
    return user.getAssignments().stream()
        .filter(
            a ->
                (hospitalId == null
                        ? a.getHospitalId() == null
                        : hospitalId.equals(a.getHospitalId()))
                    && a.getRole().getCode() == code)
        .findFirst();
  }
}
