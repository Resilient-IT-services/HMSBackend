package com.res.hms.nurse.service;

import com.res.hms.audit.service.AuditLogService;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Gender;
import com.res.hms.domain.Nurse;
import com.res.hms.nurse.dto.CreateNurseRequest;
import com.res.hms.nurse.repository.NurseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NurseServiceTest {

    @Mock
    private NurseRepository nurseRepository;
    @Mock
    private HospitalContext hospitalContext;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private NurseService nurseService;

    @Test
    void createPersistsNurse() {
        when(hospitalContext.requireHospitalId()).thenReturn(4L);
        when(nurseRepository.save(any(Nurse.class))).thenAnswer(invocation -> {
            Nurse nurse = invocation.getArgument(0);
            nurse.setId(40L);
            return nurse;
        });

        var response = nurseService.create(new CreateNurseRequest(
                "Sara", Gender.FEMALE, LocalDate.of(1990, 2, 2), "888", "ICU"
        ));

        assertEquals("Sara", response.fullName());
        verify(auditLogService).record(4L, "nurse", 40L, "CREATE", "Created nurse Sara");
    }

    @Test
    void listReturnsMappedNurses() {
        Nurse nurse = new Nurse();
        nurse.setId(4L);
        nurse.setFullName("Sara");
        nurse.setGender(Gender.FEMALE);
        nurse.setDepartment("ICU");
        when(hospitalContext.requireHospitalId()).thenReturn(4L);
        when(nurseRepository.findAllByHospitalIdOrderByCreatedAtDesc(eq(4L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(nurse)));

        var response = nurseService.list(0, 10);

        assertEquals(1, response.content().size());
        assertEquals("ICU", response.content().getFirst().department());
    }
}
