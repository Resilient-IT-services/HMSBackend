package com.res.hms.clinical.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalEntityTest {

    @Test
    void testPatientVisitFields() {
        PatientVisit v = new PatientVisit();
        v.setPatientId(UUID.randomUUID());
        v.setVisitNumber("VST-1");
        v.setVisitType(VisitType.OPD);
        v.setStatus(VisitStatus.ACTIVE);
        v.setStartedAt(LocalDateTime.now());
        v.setEndedAt(LocalDateTime.now());

        assertThat(v.getPatientId()).isNotNull();
        assertThat(v.getVisitNumber()).isEqualTo("VST-1");
        assertThat(v.getVisitType()).isEqualTo(VisitType.OPD);
        assertThat(v.getStatus()).isEqualTo(VisitStatus.ACTIVE);
        assertThat(v.getStartedAt()).isNotNull();
        assertThat(v.getEndedAt()).isNotNull();
    }
}
