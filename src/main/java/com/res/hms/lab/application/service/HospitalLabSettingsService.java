package com.res.hms.lab.application.service;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.lab.application.dto.HospitalLabDashboardStatsDTO;
import com.res.hms.lab.application.dto.HospitalLabTestConfigDTO;
import com.res.hms.lab.domain.LabHospitalTestConfig;
import com.res.hms.lab.domain.LabPrice;
import com.res.hms.lab.domain.LabTest;
import com.res.hms.lab.infrastructure.repository.LabHospitalTestConfigRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderRepository;
import com.res.hms.lab.infrastructure.repository.LabPriceRepository;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HospitalLabSettingsService {

  private final LabTestRepository testRepository;
  private final LabHospitalTestConfigRepository configRepository;
  private final LabPriceRepository priceRepository;
  private final LabOrderRepository orderRepository;
  private final HospitalContext hospitalContext;

  @Transactional(readOnly = true)
  public HospitalLabDashboardStatsDTO getDashboardStats() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);

    long activeTests = configRepository.countByHospitalIdAndActiveTrue(hospitalId);
    long pendingOrders =
        orderRepository.countByHospitalIdAndStatusAndOrderedAtGreaterThanEqual(
            hospitalId, "PENDING", todayStart);
    long processingOrders =
        orderRepository.countByHospitalIdAndStatusAndOrderedAtGreaterThanEqual(
            hospitalId, "PROCESSING", todayStart);
    long verifiedReports =
        orderRepository.countByHospitalIdAndStatusAndOrderedAtGreaterThanEqual(
            hospitalId, "VERIFIED", todayStart);

    // Simple revenue logic for MVP: Not deeply calculated right now, just returning 0
    BigDecimal revenue = BigDecimal.ZERO;

    return HospitalLabDashboardStatsDTO.builder()
        .activeLocalTests(activeTests)
        .todayPendingOrders(pendingOrders)
        .todayProcessingOrders(processingOrders)
        .todayVerifiedReports(verifiedReports)
        .todayEstimatedRevenue(revenue)
        .build();
  }

  @Transactional(readOnly = true)
  public List<HospitalLabTestConfigDTO> getAvailableGlobalTests() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    List<LabTest> globalTests = testRepository.findAll();

    return globalTests.stream()
        .map(
            test -> {
              Optional<LabHospitalTestConfig> configOpt =
                  configRepository.findByHospitalIdAndTestId(hospitalId, test.getId());
              Optional<LabPrice> priceOpt =
                  priceRepository.findByHospitalIdAndTestId(hospitalId, test.getId());

              boolean enabled = configOpt.map(LabHospitalTestConfig::getActive).orElse(false);
              Integer tatValue =
                  configOpt.map(LabHospitalTestConfig::getTatValue).orElse(test.getTatValue());
              String tatUnit =
                  configOpt.map(LabHospitalTestConfig::getTatUnit).orElse(test.getTatUnit());

              return HospitalLabTestConfigDTO.builder()
                  .testId(test.getId())
                  .testCode(test.getTestCode())
                  .testName(test.getTestName())
                  .departmentName(
                      test.getDepartment() != null ? test.getDepartment().getName() : null)
                  .enabled(enabled)
                  .tatValue(tatValue)
                  .tatUnit(tatUnit)
                  .standardPrice(priceOpt.map(LabPrice::getStandardPrice).orElse(null))
                  .emergencyPrice(priceOpt.map(LabPrice::getEmergencyPrice).orElse(null))
                  .build();
            })
        .collect(Collectors.toList());
  }

  @Transactional
  public void configureTest(HospitalLabTestConfigDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    // 1. Upsert Config
    LabHospitalTestConfig config =
        configRepository
            .findByHospitalIdAndTestId(hospitalId, dto.getTestId())
            .orElseGet(
                () -> {
                  LabHospitalTestConfig newConfig = new LabHospitalTestConfig();
                  newConfig.setHospitalId(hospitalId);
                  newConfig.setTestId(dto.getTestId());
                  return newConfig;
                });

    config.setActive(dto.isEnabled());
    config.setTatValue(dto.getTatValue());
    config.setTatUnit(dto.getTatUnit());
    configRepository.save(config);

    // 2. Upsert Price
    LabPrice price =
        priceRepository
            .findByHospitalIdAndTestId(hospitalId, dto.getTestId())
            .orElseGet(
                () -> {
                  LabPrice newPrice = new LabPrice();
                  newPrice.setHospitalId(hospitalId);
                  newPrice.setTestId(dto.getTestId());
                  newPrice.setEffectiveFrom(java.time.LocalDate.now());
                  return newPrice;
                });

    price.setStandardPrice(dto.getStandardPrice());
    price.setEmergencyPrice(dto.getEmergencyPrice());
    priceRepository.save(price);
  }
}
