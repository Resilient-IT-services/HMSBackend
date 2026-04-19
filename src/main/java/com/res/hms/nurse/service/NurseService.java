package com.res.hms.nurse.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.audit.Auditable;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Nurse;
import com.res.hms.domain.UserAccount;
import com.res.hms.nurse.dto.CreateNurseRequest;
import com.res.hms.nurse.dto.NurseResponse;
import com.res.hms.nurse.repository.NurseRepository;
import com.res.hms.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NurseService {

  private final NurseRepository nurseRepository;
  private final UserAccountRepository userAccountRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  @Transactional
  @Auditable(entity = "nurse", action = "CREATE")
  public NurseResponse create(CreateNurseRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();

    UserAccount user =
        resourceLookup.findRequired(
            request.userId(), hospitalId, userAccountRepository::findByIdAndHospitalId, "User");

    Nurse nurse = new Nurse();
    nurse.setHospitalId(hospitalId);
    nurse.setUser(user);
    nurse.setDepartment(request.department());
    nurseRepository.save(nurse);

    return toResponse(nurse);
  }

  public PageResponse<NurseResponse> list(int page, int size) {
    Long hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        nurseRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, PageRequest.of(page, size))
            .map(this::toResponse));
  }

  private NurseResponse toResponse(Nurse nurse) {
    UserAccount user = nurse.getUser();
    return new NurseResponse(
        nurse.getId(),
        user != null ? user.getFullName() : "N/A",
        user != null ? user.getGender() : null,
        user != null ? user.getDob() : null,
        user != null ? user.getPhone() : null,
        nurse.getDepartment(),
        nurse.getCreatedAt());
  }
}
