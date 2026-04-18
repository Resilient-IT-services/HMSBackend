package com.res.hms.doctor.service;

import com.res.hms.audit.service.AuditLogService;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.doctor.dto.CreateDoctorRequest;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Gender;
import com.res.hms.domain.UserAccount;
import com.res.hms.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private HospitalContext hospitalContext;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DoctorService doctorService;

    @Test
    void createPersistsDoctor() {
        UserAccount user = new UserAccount();
        user.setId(10L);
        user.setFullName("Dr. Maya");
        
        when(hospitalContext.requireHospitalId()).thenReturn(4L);
        when(userAccountRepository.findByIdAndHospitalId(10L, 4L)).thenReturn(Optional.of(user));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(invocation -> {
            Doctor doctor = invocation.getArgument(0);
            doctor.setId(30L);
            return doctor;
        });

        var response = doctorService.create(new CreateDoctorRequest(10L, "Cardiology"));

        assertEquals("Dr. Maya", response.fullName());
        verify(auditLogService).record(4L, "doctor", 30L, "CREATE", "Created doctor profile for Dr. Maya");
    }

    @Test
    void listReturnsMappedDoctors() {
        UserAccount user = new UserAccount();
        user.setFullName("Dr. Maya");

        Doctor doctor = new Doctor();
        doctor.setId(3L);
        doctor.setUser(user);
        doctor.setSpecialization("Cardiology");
        when(hospitalContext.requireHospitalId()).thenReturn(4L);
        when(doctorRepository.findAllByHospitalIdOrderByCreatedAtDesc(eq(4L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(doctor)));

        var response = doctorService.list(0, 10);

        assertEquals(1, response.content().size());
        assertEquals("Cardiology", response.content().getFirst().specialization());
    }

    @Test
    void requireDoctorRejectsMissingDoctor() {
        when(doctorRepository.findByIdAndHospitalId(12L, 4L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> doctorService.requireDoctor(12L, 4L));

        assertEquals("NOT_FOUND", exception.getCode());
    }
}
