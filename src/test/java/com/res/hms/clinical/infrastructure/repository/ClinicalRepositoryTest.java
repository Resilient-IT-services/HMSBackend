package com.res.hms.clinical.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.res.hms.TestDataHelper;
import com.res.hms.clinical.domain.PatientVisit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClinicalRepositoryTest {

    @Autowired private PatientVisitRepository patientVisitRepository;
    @Autowired private TestDataHelper testDataHelper;

    @Test
    void testPatientVisitRepositoryMethods() {
        UUID hospitalId = testDataHelper.createHospitalIfMissing("VISIT_REPO_TEST", "Visit Repo Hospital");
        UUID patientId = testDataHelper.createTestPatient(hospitalId).getId();

        PatientVisit visit = testDataHelper.createTestVisit(hospitalId, patientId);
        
        assertThat(patientVisitRepository.findByHospitalIdAndVisitNumber(hospitalId, visit.getVisitNumber())).isPresent();
        assertThat(patientVisitRepository.findAllByHospitalId(hospitalId)).isNotEmpty();
    }
}
