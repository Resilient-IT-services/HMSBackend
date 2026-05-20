package com.res.hms.clinical.application.patient.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.res.hms.TestDataHelper;
import com.res.hms.clinical.application.patient.dto.PatientDTO;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.tenant.TenantContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PatientCodeGenerationTest {

  @Autowired private PatientAggregateService patientAggregateService;
  @Autowired private PatientRepository patientRepository;
  @Autowired private HospitalContext hospitalContext;
  @Autowired private TestDataHelper testDataHelper;

  private UUID hospitalAId;
  private UUID hospitalBId;

  @BeforeEach
  void setup() {
    hospitalAId = testDataHelper.createHospitalIfMissing("HOSP_A", "Hospital A");
    hospitalBId = testDataHelper.createHospitalIfMissing("HOSP_B", "Hospital B");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void shouldGenerateUniqueCodesAcrossHospitals() {
    // 1. Register patient in Hospital A
    TenantContext.setHospitalId(hospitalAId);
    PatientDTO patientA = PatientDTO.builder().fullName("Patient A").phone("1111111111").build();
    PatientDTO savedA = patientAggregateService.savePatientAggregate(patientA);
    String codeA = savedA.getPatientCode();
    assertThat(codeA).isNotNull();

    // 2. Register patient in Hospital B
    TenantContext.setHospitalId(hospitalBId);
    PatientDTO patientB = PatientDTO.builder().fullName("Patient B").phone("2222222222").build();
    PatientDTO savedB = patientAggregateService.savePatientAggregate(patientB);
    String codeB = savedB.getPatientCode();

    assertThat(codeB).isNotNull();
    assertThat(codeB).isNotEqualTo(codeA);

    // Verify sequence
    String[] partsA = codeA.split("-");
    String[] partsB = codeB.split("-");

    int numA = Integer.parseInt(partsA[1]);
    int numB = Integer.parseInt(partsB[1]);

    assertThat(numB).isEqualTo(numA + 1);
  }
}
