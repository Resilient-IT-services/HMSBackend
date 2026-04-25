package com.res.hms.user.service;

import com.res.hms.doctor.service.DoctorService;
import com.res.hms.domain.AdminHospitalDetails;
import com.res.hms.domain.LabTechProfile;
import com.res.hms.domain.PharmacistHospitalDetails;
import com.res.hms.domain.PharmacistProfile;
import com.res.hms.domain.ReceptionistHospitalDetails;
import com.res.hms.domain.ReceptionistProfile;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.nurse.service.NurseService;
import com.res.hms.repository.AdminHospitalDetailsRepository;
import com.res.hms.repository.LabTechProfileRepository;
import com.res.hms.repository.PharmacistHospitalDetailsRepository;
import com.res.hms.repository.PharmacistProfileRepository;
import com.res.hms.repository.ReceptionistHospitalDetailsRepository;
import com.res.hms.repository.ReceptionistProfileRepository;
import com.res.hms.user.dto.AdminHospitalDetailsDTO;
import com.res.hms.user.dto.PharmacistHospitalDetailsDTO;
import com.res.hms.user.dto.ReceptionistHospitalDetailsDTO;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.dto.UserRequestDTO;
import java.time.LocalDateTime;
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
  private final PharmacistProfileRepository pharmacistProfileRepository;
  private final LabTechProfileRepository labTechProfileRepository;
  private final ReceptionistProfileRepository receptionistProfileRepository;
  private final PharmacistHospitalDetailsRepository pharmacistHospitalDetailsRepository;
  private final ReceptionistHospitalDetailsRepository receptionistHospitalDetailsRepository;
  private final AdminHospitalDetailsRepository adminHospitalDetailsRepository;

  @Transactional
  public void handleProfileUpdate(UserAccount user, UserRequestDTO request) {
    Set<RoleCode> allRoles = user.getRoleCodes();

    doctorService.updateProfile(user, request.getDoctorInfo());
    nurseService.updateProfile(user, request.getNurseInfo());
    updatePharmacistProfile(user, request.getPharmacistInfo(), allRoles);
    updateLabTechProfile(user, request.getLabTechInfo());
    updateReceptionistProfile(user, request.getReceptionistInfo(), allRoles);

    if (request.getDetailedMemberships() != null) {
      for (UserFullProfileDTO.DetailedHospitalMembershipDTO m : request.getDetailedMemberships()) {
        if (m.getRoleAssignments() != null) {
          for (UserFullProfileDTO.DetailedRoleAssignmentDTO ra : m.getRoleAssignments()) {
            updateRoleSpecificHospitalDetails(user, m.getHospitalId(), ra);
          }
        }
      }
    }
  }

  private void updatePharmacistProfile(UserAccount user, UserFullProfileDTO.PharmacistInfo info, Set<RoleCode> roles) {
    if (roles.contains(RoleCode.PHARMACIST) && info != null) {
      PharmacistProfile profile =
          pharmacistProfileRepository
              .findById(user.getId())
              .orElseGet(
                  () -> {
                    PharmacistProfile p = new PharmacistProfile();
                    p.setUser(user);
                    return p;
                  });
      if (info.getPciRegNo() != null) {
        profile.setPciRegNo(info.getPciRegNo());
      }
      if (info.getQualification() != null) {
        profile.setQualification(info.getQualification());
      }
      pharmacistProfileRepository.save(profile);
      user.setPharmacistProfile(profile);
    }
  }

  private void updateLabTechProfile(UserAccount user, UserFullProfileDTO.LabTechInfo info) {
    if (info != null) {
      LabTechProfile profile =
          labTechProfileRepository
              .findById(user.getId())
              .orElseGet(
                  () -> {
                    LabTechProfile p = new LabTechProfile();
                    p.setUser(user);
                    return p;
                  });
      if (info.getSpecialty() != null) {
        profile.setSpecialty(info.getSpecialty());
      }
      if (info.getLicenseNo() != null) {
        profile.setLicenseNo(info.getLicenseNo());
      }
      labTechProfileRepository.save(profile);
      user.setLabTechProfile(profile);
    }
  }

  private void updateReceptionistProfile(UserAccount user, UserFullProfileDTO.ReceptionistInfo info, Set<RoleCode> roles) {
    if (roles.contains(RoleCode.RECEPTIONIST) && info != null) {
      ReceptionistProfile profile =
          receptionistProfileRepository
              .findById(user.getId())
              .orElseGet(
                  () -> {
                    ReceptionistProfile p = new ReceptionistProfile();
                    p.setUser(user);
                    return p;
                  });
      if (info.getLanguagesSpoken() != null) {
        profile.setLanguagesSpoken(info.getLanguagesSpoken());
      }
      profile.setInsuranceSpecialist(info.isInsuranceSpecialist());
      receptionistProfileRepository.save(profile);
      user.setReceptionistProfile(profile);
    }
  }

  private void updateRoleSpecificHospitalDetails(
      UserAccount user, UUID hospitalId, UserFullProfileDTO.DetailedRoleAssignmentDTO raDto) {
    RoleCode code = RoleCode.valueOf(raDto.getRoleCode());
    findAssignment(user, hospitalId, code)
        .ifPresent(
            a -> {
              switch (code) {
                case DOCTOR:
                  doctorService.updateHospitalDetails(a, raDto.getDoctorDetails());
                  break;
                case NURSE:
                  nurseService.updateHospitalDetails(a, raDto.getNurseDetails());
                  break;
                case PHARMACIST:
                  updatePharmacistHospitalDetails(a, raDto.getPharmacistDetails());
                  break;
                case RECEPTIONIST:
                  updateReceptionistHospitalDetails(a, raDto.getReceptionistDetails());
                  break;
                case ADMIN:
                  updateAdminHospitalDetails(a, raDto.getAdminDetails());
                  break;
                default:
                  break;
              }
            });
  }

  private void updatePharmacistHospitalDetails(
      UserRoleAssignment a, PharmacistHospitalDetailsDTO p) {
    if (p == null) {
      return;
    }
    PharmacistHospitalDetails det =
        pharmacistHospitalDetailsRepository
            .findById(a.getId())
            .orElseGet(
                () -> {
                  PharmacistHospitalDetails newDet = new PharmacistHospitalDetails();
                  newDet.setAssignment(a);
                  return newDet;
                });
    if (p.getCounterLocation() != null) {
      det.setCounterLocation(p.getCounterLocation());
    }
    if (p.getPharmacyWing() != null) {
      det.setPharmacyWing(p.getPharmacyWing());
    }
    det.setUpdatedAt(LocalDateTime.now());
    pharmacistHospitalDetailsRepository.save(det);
    a.setPharmacistDetails(det);
  }

  private void updateReceptionistHospitalDetails(
      UserRoleAssignment a, ReceptionistHospitalDetailsDTO r) {
    if (r == null) {
      return;
    }
    ReceptionistHospitalDetails det =
        receptionistHospitalDetailsRepository
            .findById(a.getId())
            .orElseGet(
                () -> {
                  ReceptionistHospitalDetails newDet = new ReceptionistHospitalDetails();
                  newDet.setAssignment(a);
                  return newDet;
                });
    if (r.getDeskLocation() != null) {
      det.setDeskLocation(r.getDeskLocation());
    }
    if (r.getEmergencyContactNumber() != null) {
      det.setEmergencyContactNumber(r.getEmergencyContactNumber());
    }
    det.setUpdatedAt(LocalDateTime.now());
    receptionistHospitalDetailsRepository.save(det);
    a.setReceptionistDetails(det);
  }

  private void updateAdminHospitalDetails(
      UserRoleAssignment a, AdminHospitalDetailsDTO ad) {
    if (ad == null) {
      return;
    }
    AdminHospitalDetails det =
        adminHospitalDetailsRepository
            .findById(a.getId())
            .orElseGet(
                () -> {
                  AdminHospitalDetails newDet = new AdminHospitalDetails();
                  newDet.setAssignment(a);
                  return newDet;
                });
    if (ad.getOfficeLocation() != null) {
      det.setOfficeLocation(ad.getOfficeLocation());
    }
    det.setDepartmentHead(ad.isDepartmentHead());
    det.setUpdatedAt(LocalDateTime.now());
    adminHospitalDetailsRepository.save(det);
    a.setAdminDetails(det);
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
