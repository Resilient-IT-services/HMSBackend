package com.res.hms.clinical.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.res.hms.TestDataHelper;
import com.res.hms.clinical.domain.PatientVisit;
import com.res.hms.clinical.domain.VisitStatus;
import com.res.hms.clinical.domain.VisitType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PatientVisitServiceTest {

  @Autowired private PatientVisitService patientVisitService;
  @Autowired private TestDataHelper testDataHelper;

  @Test
  void testCreateAndCloseVisit() {
    UUID hospitalId =
        testDataHelper.createHospitalIfMissing("SERVICE_TEST", "Service Test Hospital");
    UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();

    PatientVisit visit = patientVisitService.createVisit(hospitalId, patientId, VisitType.OPD);
    assertThat(visit.getId()).isNotNull();
    assertThat(visit.getStatus()).isEqualTo(VisitStatus.ACTIVE);

    patientVisitService.closeVisit(visit.getId());

    assertThat(visit.getStatus()).isEqualTo(VisitStatus.DISCHARGED);
    assertThat(visit.getEndedAt()).isNotNull();
  }

  @Test
  void testCloseNonExistentVisit() {
    patientVisitService.closeVisit(UUID.randomUUID());
    // Should not throw exception
  }
}
