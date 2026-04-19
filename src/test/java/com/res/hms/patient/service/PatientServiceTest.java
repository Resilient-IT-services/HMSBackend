package com.res.hms.patient.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Gender;
import com.res.hms.domain.Patient;
import com.res.hms.patient.dto.CreatePatientRequest;
import com.res.hms.patient.repository.PatientRepository;
import java.time.LocalDate;
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
class PatientServiceTest {

  @Mock private PatientRepository patientRepository;
  @Mock private HospitalContext hospitalContext;
  @Mock private ResourceLookup resourceLookup;

  @InjectMocks private PatientService patientService;

  @Test
  void createPersistsPatientAndAudits() {
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(patientRepository.save(any(Patient.class)))
        .thenAnswer(
            invocation -> {
              Patient patient = invocation.getArgument(0);
              patient.setId(20L);
              return patient;
            });

    var response =
        patientService.create(
            new CreatePatientRequest(
                "Asha", Gender.FEMALE, LocalDate.of(1992, 8, 14), "9999999999", "Kochi"));

    assertEquals("Asha", response.fullName());
  }

  @Test
  void listReturnsMappedPatients() {
    Patient patient = new Patient();
    patient.setId(2L);
    patient.setFullName("Asha");
    patient.setGender(Gender.FEMALE);
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(patientRepository.findAllByHospitalIdOrderByCreatedAtDesc(eq(4L), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(patient)));

    var response = patientService.list(0, 10);

    assertEquals(1, response.content().size());
    assertEquals("Asha", response.content().getFirst().fullName());
  }

  @Test
  void getRejectsMissingPatient() {
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(resourceLookup.findRequired(eq(99L), eq(4L), any(), anyString()))
        .thenThrow(new ApiException("NOT_FOUND", "Patient not found", HttpStatus.NOT_FOUND));

    ApiException exception = assertThrows(ApiException.class, () -> patientService.get(99L));

    assertEquals("NOT_FOUND", exception.getCode());
  }

  @Test
  void requirePatientReturnsPatient() {
    Patient patient = new Patient();
    patient.setId(9L);
    when(resourceLookup.findRequired(eq(9L), eq(4L), any(), anyString())).thenReturn(patient);

    assertEquals(9L, patientService.requirePatient(9L, 4L).getId());
  }
}
