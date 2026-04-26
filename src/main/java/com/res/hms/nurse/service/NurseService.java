package com.res.hms.nurse.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Nurse;
import com.res.hms.domain.NurseHospitalDetails;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.nurse.dto.NurseResponse;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.NurseHospitalDetailsRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.dto.NurseHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NurseService {

  private final NurseRepository nurseRepository;
  private final NurseHospitalDetailsRepository nurseHospitalDetailsRepository;
  private final UserAccountRepository userAccountRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  @Transactional
  public void updateProfile(UserAccount user, UserDetailsDTO.NurseInfo info) {
    if (info == null) {
      return;
    }
    Nurse nurse =
        nurseRepository
            .findByUserId(user.getId())
            .orElseGet(
                () -> {
                  Nurse n = new Nurse();
                  n.setUser(user);
                  return n;
                });
    if (info.getNursingLicenseNo() != null) {
      nurse.setNursingLicenseNo(info.getNursingLicenseNo());
    }
    if (info.getCertificationLevel() != null) {
      nurse.setCertificationLevel(info.getCertificationLevel());
    }
    nurseRepository.save(nurse);
    user.setNurse(nurse);
  }

  @Transactional
  public void updateHospitalDetails(UserRoleAssignment assignment, NurseHospitalDetailsDTO n) {
    if (n == null) {
      return;
    }
    NurseHospitalDetails det =
        nurseHospitalDetailsRepository
            .findById(assignment.getId())
            .orElseGet(
                () -> {
                  NurseHospitalDetails newDet = new NurseHospitalDetails();
                  newDet.setAssignment(assignment);
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
    assignment.setNurseDetails(det);
  }

  public PageResponse<NurseResponse> list(int page, int size) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        nurseRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, PageRequest.of(page, size))
            .map(this::toResponse));
  }

  private NurseResponse toResponse(Nurse nurse) {
    UserAccount user = nurse.getUser();
    String department = null;
    UUID activeHospitalId = hospitalContext.getHospitalIdOrNull();
    if (user != null && activeHospitalId != null) {
      department =
          user.getAssignments().stream()
              .filter(a -> activeHospitalId.equals(a.getHospitalId()))
              .map(UserRoleAssignment::getDepartmentName)
              .findFirst()
              .orElse(null);
    }
    return new NurseResponse(
        nurse.getId(),
        user != null ? user.getFullName() : "N/A",
        user != null ? user.getGender() : null,
        user != null ? user.getDob() : null,
        user != null ? user.getPhone() : null,
        department,
        nurse.getCreatedAt());
  }
}
