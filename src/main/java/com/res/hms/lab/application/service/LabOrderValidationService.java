package com.res.hms.lab.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.domain.LabPackage;
import com.res.hms.lab.domain.LabPackageTest;
import com.res.hms.lab.domain.LabTest;
import com.res.hms.lab.domain.TestCategory;
import com.res.hms.lab.infrastructure.repository.LabPackageRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageTestRepository;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabOrderValidationService {

  private final LabPackageTestRepository packageTestRepository;
  private final LabTestRepository testRepository;
  private final LabPackageRepository packageRepository;

  public void validateNoDuplicates(List<UUID> testIds, List<UUID> packageIds) {
    if (testIds == null || testIds.isEmpty() || packageIds == null || packageIds.isEmpty()) {
      return;
    }

    for (UUID packageId : packageIds) {
      List<UUID> coveredTestIds =
          packageTestRepository.findByPackageId(packageId).stream()
              .map(LabPackageTest::getTestId)
              .collect(Collectors.toList());

      List<String> duplicateNames =
          testIds.stream()
              .filter(coveredTestIds::contains)
              .map(
                  testId ->
                      testRepository
                          .findById(testId)
                          .map(LabTest::getTestName)
                          .orElse(testId.toString()))
              .collect(Collectors.toList());

      if (!duplicateNames.isEmpty()) {
        String packageName =
            packageRepository
                .findById(packageId)
                .map(LabPackage::getPackageName)
                .orElse(packageId.toString());

        throw new ApiException(
            "DUPLICATE_TEST",
            String.format(
                "%s %s already included in \"%s\". Remove the duplicate test(s) before ordering.",
                String.join(", ", duplicateNames),
                duplicateNames.size() == 1 ? "is" : "are",
                packageName),
            HttpStatus.BAD_REQUEST);
      }
    }
  }

  public void validateSampleCollection(LabOrderTest orderTest) {
    if (orderTest.getCategory() != null && orderTest.getCategory() != TestCategory.LAB) {
      throw new ApiException(
          "INVALID_CATEGORY",
          "Sample collection is only allowed for LAB tests",
          HttpStatus.BAD_REQUEST);
    }

    if (!"PENDING".equals(orderTest.getStatus())) {
      throw new ApiException(
          "INVALID_STATE", "Sample already collected or processed", HttpStatus.BAD_REQUEST);
    }
  }
}
