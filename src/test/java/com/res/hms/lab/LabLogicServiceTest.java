package com.res.hms.lab;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.res.hms.lab.application.dto.LabReferenceRangeDTO;
import com.res.hms.lab.application.service.LabLogicService;
import com.res.hms.lab.application.service.LabMapper;
import com.res.hms.lab.domain.LabReferenceRange;
import com.res.hms.lab.infrastructure.repository.LabReferenceRangeRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LabLogicServiceTest {

  @Mock private LabReferenceRangeRepository rangeRepository;
  @Mock private LabMapper mapper;
  @InjectMocks private LabLogicService labLogicService;

  private UUID parameterId;

  @BeforeEach
  void setUp() {
    parameterId = UUID.randomUUID();
  }

  @Test
  @DisplayName("Logic: Should find specific Pediatric range for a 5-year old child")
  void testPediatricRangeMatching() {
    // Given: Multiple ranges for Hemoglobin
    LabReferenceRange pediatric =
        LabReferenceRange.builder()
            .parameterId(parameterId)
            .gender("ALL")
            .minAge(0)
            .maxAge(12)
            .ageUnit("YEARS")
            .minValue(new BigDecimal("11.0"))
            .maxValue(new BigDecimal("13.0"))
            .build();

    LabReferenceRange adultMale =
        LabReferenceRange.builder()
            .parameterId(parameterId)
            .gender("MALE")
            .minAge(13)
            .maxAge(100)
            .ageUnit("YEARS")
            .minValue(new BigDecimal("13.5"))
            .maxValue(new BigDecimal("17.5"))
            .build();

    when(rangeRepository.findAll()).thenReturn(Arrays.asList(pediatric, adultMale));
    when(mapper.toDto(pediatric))
        .thenReturn(
            LabReferenceRangeDTO.builder()
                .minValue(pediatric.getMinValue())
                .maxValue(pediatric.getMaxValue())
                .build());

    // When: Searching for a 5-year old male
    Optional<LabReferenceRangeDTO> matched =
        labLogicService.findRangeForPatient(parameterId, "MALE", 5, "YEARS");

    // Then: Should match pediatric range (ALL) because age matches, even if gender doesn't match
    // adultMale
    assertTrue(matched.isPresent());
    assertEquals(new BigDecimal("11.0"), matched.get().getMinValue());
  }

  @Test
  @DisplayName("Logic: Should find Gender-specific range over 'ALL' for Adults")
  void testGenderSpecificMatching() {
    LabReferenceRange adultAll =
        LabReferenceRange.builder()
            .parameterId(parameterId)
            .gender("ALL")
            .minAge(18)
            .maxAge(100)
            .ageUnit("YEARS")
            .minValue(new BigDecimal("12.0"))
            .maxValue(new BigDecimal("16.0"))
            .build();

    LabReferenceRange adultFemale =
        LabReferenceRange.builder()
            .parameterId(parameterId)
            .gender("FEMALE")
            .minAge(18)
            .maxAge(100)
            .ageUnit("YEARS")
            .minValue(new BigDecimal("11.5"))
            .maxValue(new BigDecimal("15.5"))
            .build();

    when(rangeRepository.findAll()).thenReturn(Arrays.asList(adultAll, adultFemale));
    when(mapper.toDto(adultFemale))
        .thenReturn(
            LabReferenceRangeDTO.builder()
                .minValue(adultFemale.getMinValue())
                .maxValue(adultFemale.getMaxValue())
                .build());

    // When: Searching for an Adult Female
    Optional<LabReferenceRangeDTO> matched =
        labLogicService.findRangeForPatient(parameterId, "FEMALE", 25, "YEARS");

    // Then: Priority should go to FEMALE specific range
    assertTrue(matched.isPresent());
    assertEquals(new BigDecimal("11.5"), matched.get().getMinValue());
  }

  @Test
  @DisplayName("Logic: Should detect CRITICAL status correctly")
  void testCriticalAlertLogic() {
    LabReferenceRangeDTO range =
        LabReferenceRangeDTO.builder()
            .minValue(new BigDecimal("13.0"))
            .maxValue(new BigDecimal("17.0"))
            .criticalMin(new BigDecimal("7.0"))
            .criticalMax(new BigDecimal("20.0"))
            .build();

    // Cases
    assertEquals("NORMAL", labLogicService.determineResultStatus(range, "14.0"));
    assertEquals("LOW", labLogicService.determineResultStatus(range, "10.0"));
    assertEquals("CRITICAL_LOW", labLogicService.determineResultStatus(range, "6.5"));
    assertEquals("CRITICAL_HIGH", labLogicService.determineResultStatus(range, "21.0"));
  }
}
