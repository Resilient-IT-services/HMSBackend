package com.res.hms.nurse.service;

import com.res.hms.audit.service.AuditLogService;
import com.res.hms.common.api.PageResponse;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Nurse;
import com.res.hms.nurse.dto.CreateNurseRequest;
import com.res.hms.nurse.dto.NurseResponse;
import com.res.hms.nurse.repository.NurseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NurseService {

    private final NurseRepository nurseRepository;
    private final HospitalContext hospitalContext;
    private final AuditLogService auditLogService;

    @Transactional
    public NurseResponse create(CreateNurseRequest request) {
        Long hospitalId = hospitalContext.requireHospitalId();
        Nurse nurse = new Nurse();
        nurse.setHospitalId(hospitalId);
        nurse.setFullName(request.fullName());
        nurse.setGender(request.gender());
        nurse.setDob(request.dob());
        nurse.setPhone(request.phone());
        nurse.setDepartment(request.department());
        nurseRepository.save(nurse);

        auditLogService.record(hospitalId, "nurse", nurse.getId(), "CREATE", "Created nurse " + nurse.getFullName());
        return toResponse(nurse);
    }

    public PageResponse<NurseResponse> list(int page, int size) {
        Long hospitalId = hospitalContext.requireHospitalId();
        return PageResponse.from(nurseRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, PageRequest.of(page, size))
                .map(this::toResponse));
    }

    private NurseResponse toResponse(Nurse nurse) {
        return new NurseResponse(
                nurse.getId(),
                nurse.getFullName(),
                nurse.getGender(),
                nurse.getDob(),
                nurse.getPhone(),
                nurse.getDepartment(),
                nurse.getCreatedAt()
        );
    }
}
