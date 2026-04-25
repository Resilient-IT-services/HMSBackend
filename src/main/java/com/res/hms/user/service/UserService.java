package com.res.hms.user.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.audit.Auditable;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.repository.DoctorRepository;
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
import com.res.hms.domain.Role;
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
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.AdminHospitalDetailsDTO;
import com.res.hms.user.dto.DoctorHospitalDetailsDTO;
import com.res.hms.user.dto.NurseHospitalDetailsDTO;
import com.res.hms.user.dto.PharmacistHospitalDetailsDTO;
import com.res.hms.user.dto.ReceptionistHospitalDetailsDTO;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.dto.UserRequestDTO;
import com.res.hms.user.dto.UserSummaryDTO;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

    userAccountRepository.saveAndFlush(user);
    handleProfileUpdate(user, request);
    userAccountRepository.flush();

    UserAccount refreshedUser =
        userAccountRepository
            .findById(user.getId())
            .orElseThrow(
                () ->
                    new ApiException(
                        "NOT_FOUND",
                        "User not found after save",
                        HttpStatus.INTERNAL_SERVER_ERROR));

    return userMapper.toFullProfileDTO(refreshedUser);
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

    updateUserDetails(user, request, isSuperAdmin);
    updateUserMemberships(user, request, isSuperAdmin, activeHospitalId);

    userAccountRepository.saveAndFlush(user);
    handleProfileUpdate(user, request);
    userAccountRepository.flush();

    UserAccount refreshedUser =
        userAccountRepository
            .findById(user.getId())
            .orElseThrow(
                () ->
                    new ApiException(
                        "NOT_FOUND",
                        "User not found after save",
                        HttpStatus.INTERNAL_SERVER_ERROR));

    return userMapper.toFullProfileDTO(refreshedUser);
  }

  private void updateUserDetails(UserAccount user, UserRequestDTO request, boolean isSuperAdmin) {
    if (request.getFullName() != null) {
      user.setFullName(request.getFullName());
    }
    if (request.getGender() != null) {
      user.setGender(request.getGender());
    }
    if (request.getDob() != null) {
      user.setDob(request.getDob());
    }
    if (request.getPhone() != null) {
      user.setPhone(request.getPhone());
    }
    if (request.getEmail() != null) {
      user.setEmail(request.getEmail());
    }
    if (isSuperAdmin && request.getSuperuser() != null) {
      user.setSuperuser(request.getSuperuser());
    }
  }

  private void updateUserMemberships(
      UserAccount user, UserRequestDTO request, boolean isSuperAdmin, UUID activeHospitalId) {
    if (request.getDetailedMemberships() == null) {
      return;
    }

    if (!isSuperAdmin) {
      user.getAssignments().removeIf(a -> activeHospitalId.equals(a.getHospitalId()));
      request.getDetailedMemberships().stream()
          .filter(m -> activeHospitalId.equals(m.getHospitalId()))
          .flatMap(m -> m.getRoleAssignments().stream())
          .forEach(
              ra -> assignRole(user, activeHospitalId, ra.getRoleCode(), ra.getDepartmentName()));
    } else {
      user.getAssignments().clear();
      for (UserFullProfileDTO.DetailedHospitalMembershipDTO m : request.getDetailedMemberships()) {
        if (m.getRoleAssignments() != null) {
          m.getRoleAssignments()
              .forEach(
                  ra -> assignRole(user, m.getHospitalId(), ra.getRoleCode(), ra.getDepartmentName()));
        }
      }
    }
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

    updateDoctorProfile(user, request, allRoles);
    updateNurseProfile(user, request, allRoles);
    updatePharmacistProfile(user, request, allRoles);
    updateLabTechProfile(user, request);
    updateReceptionistProfile(user, request, allRoles);

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

  private void updateDoctorProfile(UserAccount user, UserRequestDTO request, Set<RoleCode> roles) {
    if (roles.contains(RoleCode.DOCTOR) && request.getDoctorInfo() != null) {
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
      if (info.getSpecialization() != null) {
        doctor.setSpecialization(info.getSpecialization());
      }
      if (info.getBio() != null) {
        doctor.setBio(info.getBio());
      }
      if (info.getLicenseNumber() != null) {
        doctor.setLicenseNumber(info.getLicenseNumber());
      }
      doctorRepository.save(doctor);
      user.setDoctor(doctor);
    }
  }

  private void updateNurseProfile(UserAccount user, UserRequestDTO request, Set<RoleCode> roles) {
    if (roles.contains(RoleCode.NURSE) && request.getNurseInfo() != null) {
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
      if (info.getDepartment() != null) {
        nurse.setDepartment(info.getDepartment());
      }
      if (info.getNursingLicenseNo() != null) {
        nurse.setNursingLicenseNo(info.getNursingLicenseNo());
      }
      if (info.getCertificationLevel() != null) {
        nurse.setCertificationLevel(info.getCertificationLevel());
      }
      nurseRepository.save(nurse);
      user.setNurse(nurse);
    }
  }

  private void updatePharmacistProfile(UserAccount user, UserRequestDTO request, Set<RoleCode> roles) {
    if (roles.contains(RoleCode.PHARMACIST) && request.getPharmacistInfo() != null) {
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

  private void updateLabTechProfile(UserAccount user, UserRequestDTO request) {
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

  private void updateReceptionistProfile(UserAccount user, UserRequestDTO request, Set<RoleCode> roles) {
    if (roles.contains(RoleCode.RECEPTIONIST) && request.getReceptionistInfo() != null) {
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
                  updateDoctorHospitalDetails(a, raDto.getDoctorDetails());
                  break;
                case NURSE:
                  updateNurseHospitalDetails(a, raDto.getNurseDetails());
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

  private void updateDoctorHospitalDetails(
      UserRoleAssignment a, DoctorHospitalDetailsDTO d) {
    if (d == null) {
      return;
    }
    DoctorHospitalDetails det =
        doctorHospitalDetailsRepository
            .findById(a.getId())
            .orElseGet(
                () -> {
                  DoctorHospitalDetails newDet = new DoctorHospitalDetails();
                  newDet.setAssignment(a);
                  return newDet;
                });
    if (d.getConsultationFee() != null) {
      det.setConsultationFee(d.getConsultationFee());
    }
    if (d.getFollowUpDays() != null) {
      det.setFollowUpDays(d.getFollowUpDays());
    }
    if (d.getCommissionType() != null) {
      det.setCommissionType(d.getCommissionType());
    }
    if (d.getCommissionValue() != null) {
      det.setCommissionValue(d.getCommissionValue());
    }
    det.setVisitingConsultant(d.isVisitingConsultant());
    det.setUpdatedAt(LocalDateTime.now());
    doctorHospitalDetailsRepository.save(det);
    a.setDoctorDetails(det);
  }

  private void updateNurseHospitalDetails(
      UserRoleAssignment a, NurseHospitalDetailsDTO n) {
    if (n == null) {
      return;
    }
    NurseHospitalDetails det =
        nurseHospitalDetailsRepository
            .findById(a.getId())
            .orElseGet(
                () -> {
                  NurseHospitalDetails newDet = new NurseHospitalDetails();
                  newDet.setAssignment(a);
                  return newDet;
                });
    if (n.getFloorLocation() != null) {
      det.setFloorLocation(n.getFloorLocation());
    }
    if (n.getShiftType() != null) {
      det.setShiftType(n.getShiftType());
    }
    det.setUpdatedAt(LocalDateTime.now());
    nurseHospitalDetailsRepository.save(det);
    a.setNurseDetails(det);
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

  @Transactional(readOnly = true)
  public PageResponse<UserSummaryDTO> listUsers(
      int page, int size, String search, String role, Boolean active, boolean includeDeleted) {
    UUID hospitalId = hospitalContext.getHospitalIdOrNull();
    boolean isSuperAdmin = hospitalContext.isSuperAdmin();

    if (hospitalId == null && !isSuperAdmin) {
      throw new ApiException(
          "BUSINESS_RULE_VIOLATION", "Please select a hospital first", HttpStatus.BAD_REQUEST);
    }

    RoleCode roleCode = null;
    if (role != null && !role.isBlank()) {
      try {
        roleCode = RoleCode.valueOf(role.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new ApiException("VALIDATION_ERROR", "Invalid role code: " + role, HttpStatus.BAD_REQUEST);
      }
    }

    Boolean deleted = includeDeleted ? null : false;
    String searchPattern = (search != null && !search.isBlank())
        ? "%" + search.trim().toLowerCase() + "%"
        : null;

    return PageResponse.from(
        userAccountRepository
            .searchUsers(
                hospitalId, active, deleted, roleCode, searchPattern, PageRequest.of(page, size))
            .map(userMapper::toSummaryDTO));
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
