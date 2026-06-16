package com.res.hms.pharmacy.application.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class PharmacyTemplateServiceTest {

  @Test
  void testGenerateMedicineTemplate() throws IOException {
    byte[] template = PharmacyTemplateService.generateMedicineTemplate();
    assertNotNull(template);
    assertTrue(template.length > 0);
  }

  @Test
  void testGenerateConsumableTemplate() throws IOException {
    byte[] template = PharmacyTemplateService.generateConsumableTemplate();
    assertNotNull(template);
    assertTrue(template.length > 0);
  }
}
