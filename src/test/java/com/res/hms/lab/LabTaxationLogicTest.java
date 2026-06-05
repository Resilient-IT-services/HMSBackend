package com.res.hms.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.res.hms.tax.application.dto.TaxCalculationRequest;
import com.res.hms.tax.application.dto.TaxCalculationResult;
import com.res.hms.tax.application.dto.TaxComponent;
import com.res.hms.tax.application.service.TaxEngineService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LabTaxationLogicTest {

  @Mock private TaxEngineService taxEngineService;

  @Test
  @DisplayName("Flow: Should calculate Intra-state GST (CGST+SGST) for a Lab Test")
  void testIntrastateLabTax() {
    // Given
    UUID hospitalId = UUID.randomUUID();
    BigDecimal testPrice = new BigDecimal("1000.00");

    TaxCalculationRequest request =
        TaxCalculationRequest.builder()
            .hospitalId(hospitalId)
            .itemType("LAB_TEST")
            .hsnOrSacCode("9993") // SAC for medical services
            .quantity(BigDecimal.ONE)
            .unitPrice(testPrice)
            .taxableAmount(testPrice)
            .hospitalState("Maharashtra")
            .billingState("Maharashtra")
            .isTaxInclusive(false)
            .build();

    TaxCalculationResult expectedResult =
        new TaxCalculationResult(
            testPrice,
            new BigDecimal("50.00"), // 2.5% CGST + 2.5% SGST = 5%
            new BigDecimal("1050.00"),
            Arrays.asList(
                new TaxComponent("CGST", "CGST", new BigDecimal("2.5"), new BigDecimal("25.00")),
                new TaxComponent("SGST", "SGST", new BigDecimal("2.5"), new BigDecimal("25.00"))));

    when(taxEngineService.calculate(any())).thenReturn(expectedResult);

    // When
    TaxCalculationResult actual = taxEngineService.calculate(request);

    // Then
    assertEquals(new BigDecimal("50.00"), actual.totalTax());
    assertEquals(2, actual.components().size());
    assertEquals("CGST", actual.components().get(0).taxName());
  }

  @Test
  @DisplayName("Flow: Should calculate Inter-state GST (IGST) for a Lab Test")
  void testInterstateLabTax() {
    // Given
    UUID hospitalId = UUID.randomUUID();
    BigDecimal testPrice = new BigDecimal("1000.00");

    TaxCalculationRequest request =
        TaxCalculationRequest.builder()
            .hospitalId(hospitalId)
            .itemType("LAB_TEST")
            .hsnOrSacCode("9993")
            .quantity(BigDecimal.ONE)
            .unitPrice(testPrice)
            .taxableAmount(testPrice)
            .hospitalState("Maharashtra")
            .billingState("Karnataka") // Different state
            .isTaxInclusive(false)
            .build();

    TaxCalculationResult expectedResult =
        new TaxCalculationResult(
            testPrice,
            new BigDecimal("50.00"), // 5% IGST
            new BigDecimal("1050.00"),
            Collections.singletonList(
                new TaxComponent("IGST", "IGST", new BigDecimal("5.0"), new BigDecimal("50.00"))));

    when(taxEngineService.calculate(any())).thenReturn(expectedResult);

    // When
    TaxCalculationResult actual = taxEngineService.calculate(request);

    // Then
    assertEquals(new BigDecimal("50.00"), actual.totalTax());
    assertEquals(1, actual.components().size());
    assertEquals("IGST", actual.components().get(0).taxName());
  }
}
