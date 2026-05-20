package com.res.hms.clinical.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalEntityTest {

    @Test
    void testPatientVisitFields() {
        PatientVisit v = new PatientVisit();
        v.setPatientId(UUID.randomUUID());
        v.setVisitNumber("VST-123");
        v.setVisitType(VisitType.OPD);
        v.setStatus(VisitStatus.ACTIVE);
        v.setStartedAt(java.time.Instant.now());
        v.setEndedAt(java.time.Instant.now());

        assertThat(v.getPatientId()).isNotNull();
        assertThat(v.getVisitNumber()).isEqualTo("VST-123");
        assertThat(v.getVisitType()).isEqualTo(VisitType.OPD);
        assertThat(v.getStatus()).isEqualTo(VisitStatus.ACTIVE);
        assertThat(v.getStartedAt()).isNotNull();
        assertThat(v.getEndedAt()).isNotNull();
    }
}
