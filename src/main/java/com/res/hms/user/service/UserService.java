package com.res.hms.user.service;

import com.res.hms.common.audit.Auditable;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.*;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.*;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.dto.UserRequestDTO;
import com.res.hms.user.dto.UserSummaryDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.AvoidInstantiatingObjectsInLoops"})
public class UserService {

  private final UserAccountRepository userAccountRepository;
  private final RoleRepository roleRepository;
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
  private final PasswordEncoder passwordEncoder;
  private final HospitalContext hospitalContext;
  private final UserMapper userMapper;
  private final ResourceLookup resourceLookup;

  @Transactional
  @Auditable(entity = "user_account", action = "CREATE")
  public UserFullProfileDTO createUser(UserRequestDTO request) {
    UUID activeHospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isCallerSuperAdmin = hospitalContext.isSuperAdmin();

    validateCreateRequest(request);

    UserAccount user = new UserAccount();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setFullName(request.getFullName());
    user.setGender(request.getGender());
    user.setDob(request.getDob());
    user.setPhone(request.getPhone());
    user.setActive(true);

    if (isCallerSuperAdmin && request.getSuperuser() != null) {
      user.setSuperuser(request.getSuperuser());
    }

    if (request.getDetailedMemberships() != null) {
      for (UserFullProfileDTO.DetailedHospitalMembershipDTO memRequest :
          request.getDetailedMemberships()) {
        UUID hospitalId = memRequest.getHospitalId();
        // If caller is not super admin, they can only create assignments in their active hospital
        if (!isCallerSuperAdmin && activeHospitalId != null) {
          hospitalId = activeHospitalId;
        }

        if (hospitalId != null && memRequest.getRoleAssignments() != null) {
          for (UserFullProfileDTO.DetailedRoleAssignmentDTO ra : memRequest.getRoleAssignments()) {
            assignRole(user, hospitalId, ra.getRoleCode(), ra.getDepartmentName());
          }
        }
      }
    }

    userAccountRepository.save(user);
    handleProfileUpdate(user, request);

    return userMapper.toFullProfileDTO(user);
  }

  @Transactional
  @Auditable(entity = "user_account", action = "UPDATE")
  public UserFullProfileDTO updateUser(UUID userId, UserRequestDTO request) {
    UUID activeHospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isSuperAdmin = hospitalContext.isSuperAdmin();

    UserAccount user;
    if (isSuperAdmin) {
      user = resourceLookup.findRequired(userId, userAccountRepository::findById, "User");
    } else {
      if (activeHospitalId == null) {
        throw new ApiException(
            "AUTH_ERROR", "Hospital context required for update", HttpStatus.FORBIDDEN);
      }
      user =
          resourceLookup.findRequired(
              userId, activeHospitalId, userAccountRepository::findByIdAndHospitalId, "User");
    }

    if (request.getFullName() != null) user.setFullName(request.getFullName());
    if (request.getGender() != null) user.setGender(request.getGender());
    if (request.getDob() != null) user.setDob(request.getDob());
    if (request.getPhone() != null) user.setPhone(request.getPhone());
    if (request.getEmail() != null) user.setEmail(request.getEmail());

    if (isSuperAdmin && request.getSuperuser() != null) {
      user.setSuperuser(request.getSuperuser());
    }

    // Scoped Update Logic for Memberships
    if (request.getDetailedMemberships() != null) {
      if (!isSuperAdmin) {
        // Remove existing assignments ONLY for the active hospital
        user.getAssignments().removeIf(a -> activeHospitalId.equals(a.getHospitalId()));

        // Add new assignments for the active hospital from the request
        request.getDetailedMemberships().stream()
            .filter(m -> activeHospitalId.equals(m.getHospitalId()))
            .flatMap(m -> m.getRoleAssignments().stream())
            .forEach(
                ra -> assignRole(user, activeHospitalId, ra.getRoleCode(), ra.getDepartmentName()));
      } else {
        // SuperAdmin can update everything.
        user.getAssignments().clear();
        for (UserFullProfileDTO.DetailedHospitalMembershipDTO m :
            request.getDetailedMemberships()) {
          if (m.getRoleAssignments() != null) {
            m.getRoleAssignments()
                .forEach(
                    ra ->
                        assignRole(
                            user, m.getHospitalId(), ra.getRoleCode(), ra.getDepartmentName()));
          }
        }
      }
    }

    userAccountRepository.save(user);
    handleProfileUpdate(user, request);

    return userMapper.toFullProfileDTO(user);
  }

  private void assignRole(
      UserAccount user, UUID hospitalId, String roleCode, String departmentName) {
    RoleCode code = RoleCode.valueOf(roleCode);
    Role role = resourceLookup.findRequired(code, roleRepository::findByCode, "Role");
    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.setUser(user);
    assignment.setHospitalId(hospitalId);
    assignment.setRole(role);
    assignment.setDepartmentName(departmentName);
    user.getAssignments().add(assignment);
  }

  private void handleProfileUpdate(UserAccount user, UserRequestDTO request) {
    Set<RoleCode> allRoles = user.getRoleCodes();

    // 1. Doctor Profile
    if (allRoles.contains(RoleCode.DOCTOR) && request.getDoctorInfo() != null) {
      Doctor doctor =
          doctorRepository
              .findByUserId(user.getId())
              .orElseGet(
                  () -> {
                    Doctor d = new Doctor();
                    d.setUser(user);
                    return d;
                  });
      UserFullProfileDTO.DoctorInfo info = request.getDoctorInfo();
      if (info.getSpecialization() != null) doctor.setSpecialization(info.getSpecialization());
      if (info.getBio() != null) doctor.setBio(info.getBio());
      if (info.getLicenseNumber() != null) doctor.setLicenseNumber(info.getLicenseNumber());
      doctorRepository.save(doctor);
    }

    // 2. Nurse Profile
    if (allRoles.contains(RoleCode.NURSE) && request.getNurseInfo() != null) {
      Nurse nurse =
          nurseRepository
              .findByUserId(user.getId())
              .orElseGet(
                  () -> {
                    Nurse n = new Nurse();
                    n.setUser(user);
                    return n;
                  });
      UserFullProfileDTO.NurseInfo info = request.getNurseInfo();
      if (info.getDepartment() != null) nurse.setDepartment(info.getDepartment());
      if (info.getNursingLicenseNo() != null) nurse.setNursingLicenseNo(info.getNursingLicenseNo());
      if (info.getCertificationLevel() != null)
        nurse.setCertificationLevel(info.getCertificationLevel());
      nurseRepository.save(nurse);
    }

    // 3. Pharmacist Profile
    if (allRoles.contains(RoleCode.PHARMACIST) && request.getPharmacistInfo() != null) {
      PharmacistProfile profile =
          pharmacistProfileRepository
              .findById(user.getId())
              .orElseGet(
                  () -> {
                    PharmacistProfile p = new PharmacistProfile();
                    p.setUser(user);
                    return p;
                  });
      UserFullProfileDTO.PharmacistInfo info = request.getPharmacistInfo();
      if (info.getPciRegNo() != null) profile.setPciRegNo(info.getPciRegNo());
      if (info.getQualification() != null) profile.setQualification(info.getQualification());
      pharmacistProfileRepository.save(profile);
    }

    // 4. Lab Tech Profile
    if (request.getLabTechInfo() != null) {
      LabTechProfile profile =
          labTechProfileRepository
              .findById(user.getId())
              .orElseGet(
                  () -> {
                    LabTechProfile p = new LabTechProfile();
                    p.setUser(user);
                    return p;
                  });
      UserFullProfileDTO.LabTechInfo info = request.getLabTechInfo();
      if (info.getSpecialty() != null) profile.setSpecialty(info.getSpecialty());
      if (info.getLicenseNo() != null) profile.setLicenseNo(info.getLicenseNo());
      labTechProfileRepository.save(profile);
    }

    // 5. Receptionist Profile
    if (allRoles.contains(RoleCode.RECEPTIONIST) && request.getReceptionistInfo() != null) {
      ReceptionistProfile profile =
          receptionistProfileRepository
              .findById(user.getId())
              .orElseGet(
                  () -> {
                    ReceptionistProfile p = new ReceptionistProfile();
                    p.setUser(user);
                    return p;
                  });
      UserFullProfileDTO.ReceptionistInfo info = request.getReceptionistInfo();
      if (info.getLanguagesSpoken() != null) profile.setLanguagesSpoken(info.getLanguagesSpoken());
      profile.setInsuranceSpecialist(info.isInsuranceSpecialist());
      receptionistProfileRepository.save(profile);
    }

    // 6. Hospital Specific Details (Nested in detailedMemberships)
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

  private void updateRoleSpecificHospitalDetails(
      UserAccount user, UUID hospitalId, UserFullProfileDTO.DetailedRoleAssignmentDTO raDto) {
    RoleCode code = RoleCode.valueOf(raDto.getRoleCode());
    findAssignment(user, hospitalId, code)
        .ifPresent(
            a -> {
              switch (code) {
                case DOCTOR -> {
                  if (raDto.getDoctorDetails() != null) {
                    DoctorHospitalDetails det =
                        doctorHospitalDetailsRepository
                            .findById(a.getId())
                            .orElseGet(
                                () -> {
                                  DoctorHospitalDetails d = new DoctorHospitalDetails();
                                  d.setAssignment(a);
                                  return d;
                                });
                    var d = raDto.getDoctorDetails();
                    if (d.getConsultationFee() != null)
                      det.setConsultationFee(d.getConsultationFee());
                    if (d.getFollowUpDays() != null) det.setFollowUpDays(d.getFollowUpDays());
                    if (d.getCommissionType() != null) det.setCommissionType(d.getCommissionType());
                    if (d.getCommissionValue() != null)
                      det.setCommissionValue(d.getCommissionValue());
                    det.setVisitingConsultant(d.isVisitingConsultant());
                    det.setUpdatedAt(LocalDateTime.now());
                    doctorHospitalDetailsRepository.save(det);
                  }
                }
                case NURSE -> {
                  if (raDto.getNurseDetails() != null) {
                    NurseHospitalDetails det =
                        nurseHospitalDetailsRepository
                            .findById(a.getId())
                            .orElseGet(
                                () -> {
                                  NurseHospitalDetails d = new NurseHospitalDetails();
                                  d.setAssignment(a);
                                  return d;
                                });
                    var n = raDto.getNurseDetails();
                    if (n.getFloorLocation() != null) det.setFloorLocation(n.getFloorLocation());
                    if (n.getShiftType() != null) det.setShiftType(n.getShiftType());
                    det.setUpdatedAt(LocalDateTime.now());
                    nurseHospitalDetailsRepository.save(det);
                  }
                }
                case PHARMACIST -> {
                  if (raDto.getPharmacistDetails() != null) {
                    PharmacistHospitalDetails det =
                        pharmacistHospitalDetailsRepository
                            .findById(a.getId())
                            .orElseGet(
                                () -> {
                                  PharmacistHospitalDetails d = new PharmacistHospitalDetails();
                                  d.setAssignment(a);
                                  return d;
                                });
                    var p = raDto.getPharmacistDetails();
                    if (p.getCounterLocation() != null)
                      det.setCounterLocation(p.getCounterLocation());
                    if (p.getPharmacyWing() != null) det.setPharmacyWing(p.getPharmacyWing());
                    det.setUpdatedAt(LocalDateTime.now());
                    pharmacistHospitalDetailsRepository.save(det);
                  }
                }
                case RECEPTIONIST -> {
                  if (raDto.getReceptionistDetails() != null) {
                    ReceptionistHospitalDetails det =
                        receptionistHospitalDetailsRepository
                            .findById(a.getId())
                            .orElseGet(
                                () -> {
                                  ReceptionistHospitalDetails d = new ReceptionistHospitalDetails();
                                  d.setAssignment(a);
                                  return d;
                                });
                    var r = raDto.getReceptionistDetails();
                    if (r.getDeskLocation() != null) det.setDeskLocation(r.getDeskLocation());
                    if (r.getEmergencyContactNumber() != null)
                      det.setEmergencyContactNumber(r.getEmergencyContactNumber());
                    det.setUpdatedAt(LocalDateTime.now());
                    receptionistHospitalDetailsRepository.save(det);
                  }
                }
                case ADMIN -> {
                  if (raDto.getAdminDetails() != null) {
                    AdminHospitalDetails det =
                        adminHospitalDetailsRepository
                            .findById(a.getId())
                            .orElseGet(
                                () -> {
                                  AdminHospitalDetails d = new AdminHospitalDetails();
                                  d.setAssignment(a);
                                  return d;
                                });
                    var ad = raDto.getAdminDetails();
                    if (ad.getOfficeLocation() != null)
                      det.setOfficeLocation(ad.getOfficeLocation());
                    det.setDepartmentHead(ad.isDepartmentHead());
                    det.setUpdatedAt(LocalDateTime.now());
                    adminHospitalDetailsRepository.save(det);
                  }
                }
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

  @Transactional(readOnly = true)
  public List<UserSummaryDTO> listUsers(boolean includeDeleted) {
    UUID hospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isSuperAdmin = hospitalContext.isSuperAdmin();

    if (hospitalId == null && !isSuperAdmin) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
    }

    List<UserAccount> users;
    if (isSuperAdmin && hospitalId == null) {
      users =
          includeDeleted
              ? userAccountRepository.findAll()
              : userAccountRepository.findAllByDeletedFalse();
    } else {
      users = userAccountRepository.findAllByHospitalId(hospitalId);
    }

    return users.stream().map(userMapper::toSummaryDTO).toList();
  }

  @Transactional(readOnly = true)
  public UserFullProfileDTO getFullProfile(UUID userId) {
    UserAccount user = resourceLookup.findRequired(userId, userAccountRepository::findById, "User");
    return userMapper.toFullProfileDTO(user);
  }

  private void validateCreateRequest(UserRequestDTO request) {
    if (userAccountRepository.existsByUsername(request.getUsername())) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Username already exists", HttpStatus.CONFLICT);
    }
    if (request.getDetailedMemberships() == null || request.getDetailedMemberships().isEmpty()) {
      if (request.getSuperuser() == null || !request.getSuperuser()) {
        throw new ApiException(
            "VALIDATION_ERROR",
            "At least one hospital membership is required for non-superusers",
            HttpStatus.BAD_REQUEST);
      }
    }
  }

  @Transactional
  public void deleteUser(UUID id) {
    UserAccount user = resourceLookup.findRequired(id, userAccountRepository::findById, "User");
    user.setDeleted(true);
    userAccountRepository.save(user);
  }
}
