package com.res.hms.clinical.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClinicalEnumTest {

  @Test
  void testVisitTypeEnum() {
    assertThat(VisitType.valueOf("OPD")).isEqualTo(VisitType.OPD);
    assertThat(VisitType.values()).hasSize(4);
  }

  @Test
  void testVisitStatusEnum() {
    assertThat(VisitStatus.valueOf("ACTIVE")).isEqualTo(VisitStatus.ACTIVE);
    assertThat(VisitStatus.values()).hasSize(3);
  }
}
