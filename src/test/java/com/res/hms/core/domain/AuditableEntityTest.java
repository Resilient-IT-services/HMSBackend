package com.res.hms.core.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.res.hms.TestDataHelper;
import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditableEntityTest {

  @Autowired private PatientRepository patientRepository;
  @Autowired private TestDataHelper testDataHelper;

  @Test
  void testAuditFields() {
    UUID hospitalId = testDataHelper.createHospitalIfMissing("AUDIT_TEST", "Audit Hospital");
    Patient p = new Patient();
    p.setHospitalId(hospitalId);
    p.setFullName("Audit Patient");
    p.setPatientCode("AUD-1");

    p = patientRepository.saveAndFlush(p);

    assertThat(p.getCreatedAt()).isNotNull();
    assertThat(p.getUpdatedAt()).isNotNull();

    p.setFullName("Updated Name");
    p = patientRepository.saveAndFlush(p);

    assertThat(p.getUpdatedAt()).isAfterOrEqualTo(p.getCreatedAt());
  }
}
