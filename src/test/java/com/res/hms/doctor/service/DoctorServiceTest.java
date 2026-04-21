package com.res.hms.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.dto.CreateDoctorRequest;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.UserAccount;
import com.res.hms.repository.UserAccountRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

  @Mock private DoctorRepository doctorRepository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private HospitalContext hospitalContext;
  @Mock private ResourceLookup resourceLookup;

  @InjectMocks private DoctorService doctorService;

  @Test
  void createPersistsDoctor() {
    UserAccount user = new UserAccount();
    user.setId(10L);
    user.setFullName("Dr. Maya");

    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(resourceLookup.findRequired(eq(10L), eq(4L), any(), anyString())).thenReturn(user);
    when(doctorRepository.save(any(Doctor.class)))
        .thenAnswer(
            invocation -> {
              Doctor doctor = invocation.getArgument(0);
              doctor.setId(30L);
              return doctor;
            });

    var response = doctorService.create(new CreateDoctorRequest(10L, "Cardiology"));

    assertEquals("Dr. Maya", response.fullName());
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

    var response = doctorService.list(0, 10, null);

    assertEquals(1, response.content().size());
    assertEquals("Cardiology", response.content().getFirst().specialization());
  }

  @Test
  void requireDoctorRejectsMissingDoctor() {
    when(resourceLookup.findRequired(eq(12L), eq(4L), any(), anyString()))
        .thenThrow(new ApiException("NOT_FOUND", "Doctor not found", HttpStatus.NOT_FOUND));

    ApiException exception =
        assertThrows(ApiException.class, () -> doctorService.requireDoctor(12L, 4L));

    assertEquals("NOT_FOUND", exception.getCode());
  }
}
