package com.res.hms.lab.application.service;

import com.res.hms.lab.application.dto.LabReferenceRangeDTO;
import com.res.hms.lab.domain.LabReferenceRange;
import com.res.hms.lab.infrastructure.repository.LabReferenceRangeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabLogicService {

  private final LabReferenceRangeRepository rangeRepository;
  private final LabMapper mapper;

  /**
   * Logic: Find the most specific reference range for a patient. 1. Matches Gender (or 'ALL') 2.
   * Matches Age Range 3. Priority to specific gender over 'ALL'
   */
  @Transactional(readOnly = true)
  public Optional<LabReferenceRangeDTO> findRangeForPatient(
      UUID parameterId, String gender, int age, String ageUnit) {
    List<LabReferenceRange> allRanges =
        rangeRepository.findAll().stream()
            .filter(r -> r.getParameterId().equals(parameterId))
            .filter(r -> r.getAgeUnit().equalsIgnoreCase(ageUnit))
            .filter(r -> r.getMinAge() <= age && r.getMaxAge() >= age)
            .toList();

    // 1. Try exact gender match
    Optional<LabReferenceRange> specific =
        allRanges.stream().filter(r -> r.getGender().equalsIgnoreCase(gender)).findFirst();

    if (specific.isPresent()) {
      return specific.map(mapper::toDto);
    }

    // 2. Fallback to 'ALL'
    return allRanges.stream()
        .filter(r -> r.getGender().equalsIgnoreCase("ALL"))
        .findFirst()
        .map(mapper::toDto);
  }

  /** Logic: Determine result status (NORMAL, ABNORMAL, CRITICAL) */
  public String determineResultStatus(LabReferenceRangeDTO range, String value) {
    if (value == null || range == null) {
      return "PENDING";
    }

    try {
      BigDecimal val = new BigDecimal(value);

      // Check Critical
      if (range.getCriticalMin() != null && val.compareTo(range.getCriticalMin()) <= 0) {
        return "CRITICAL_LOW";
      }
      if (range.getCriticalMax() != null && val.compareTo(range.getCriticalMax()) >= 0) {
        return "CRITICAL_HIGH";
      }

      // Check Normal
      if (val.compareTo(range.getMinValue()) < 0) {
        return "LOW";
      }
      if (val.compareTo(range.getMaxValue()) > 0) {
        return "HIGH";
      }

      return "NORMAL";
    } catch (NumberFormatException e) {
      return "TEXT_RESULT";
    }
  }
}
