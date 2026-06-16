package com.res.hms.lab.application.service;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.lab.application.dto.LabResultSaveRequest;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.domain.LabResult;
import com.res.hms.lab.domain.LabResultParameter;
import com.res.hms.lab.domain.LabResultVerification;
import com.res.hms.lab.domain.LabSample;
import com.res.hms.lab.domain.TestCategory;
import com.res.hms.lab.infrastructure.repository.LabResultParameterRepository;
import com.res.hms.lab.infrastructure.repository.LabResultRepository;
import com.res.hms.lab.infrastructure.repository.LabResultVerificationRepository;
import com.res.hms.lab.infrastructure.repository.LabSampleRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabResultService {

  private final LabResultRepository resultRepository;
  private final LabResultParameterRepository resultParameterRepository;
  private final LabSampleRepository sampleRepository;
  private final LabResultVerificationRepository verificationRepository;
  private final HospitalContext hospitalContext;

  @Transactional
  public LabResult saveOrUpdateLabResult(LabOrderTest orderTest, String findings) {
    UUID sampleId = resolveSampleId(orderTest);

    LabResult result =
        resultRepository.findAll().stream()
            .filter(r -> orderTest.getId().equals(r.getOrderTestId()))
            .findFirst()
            .orElse(new LabResult());

    result.setOrderTestId(orderTest.getId());
    result.setSampleId(sampleId);
    result.setEnteredBy(hospitalContext.requireUserId());
    result.setEnteredAt(Instant.now());
    result.setFindings(findings);
    result = resultRepository.save(result);

    return result;
  }

  @Transactional
  public void processResultParameters(
      UUID resultId, List<LabResultSaveRequest.ParameterResultDTO> results) {
    if (results == null) {
      return;
    }

    for (LabResultSaveRequest.ParameterResultDTO resDto : results) {
      LabResultParameter paramResult =
          resultParameterRepository.findAll().stream()
              .filter(
                  rp ->
                      resultId.equals(rp.getResultId())
                          && resDto.getParameterId().equals(rp.getParameterId()))
              .findFirst()
              .orElse(new LabResultParameter());

      paramResult.setResultId(resultId);
      paramResult.setParameterId(resDto.getParameterId());
      paramResult.setResultValue(resDto.getValue());
      paramResult.setAbnormal(resDto.getAbnormal());
      paramResult.setCritical(resDto.getCritical());
      paramResult.setResultStatus("COMPLETED");
      resultParameterRepository.save(paramResult);
    }
  }

  @Transactional
  public LabResult findResultByOrderTestId(UUID orderTestId) {
    return resultRepository.findAll().stream()
        .filter(r -> orderTestId.equals(r.getOrderTestId()))
        .findFirst()
        .orElse(null);
  }

  @Transactional
  public void saveVerification(UUID resultId, String status, String remarks) {
    LabResultVerification verification =
        LabResultVerification.builder()
            .resultId(resultId)
            .verifiedBy(hospitalContext.requireUserId())
            .verifiedAt(Instant.now())
            .status(status)
            .remarks(remarks)
            .build();
    verificationRepository.save(verification);
  }

  private UUID resolveSampleId(LabOrderTest orderTest) {
    if (orderTest.getCategory() == TestCategory.LAB) {
      return sampleRepository.findAll().stream()
          .filter(s -> orderTest.getId().equals(s.getOrderTestId()))
          .findFirst()
          .map(LabSample::getId)
          .orElseThrow(() -> new RuntimeException("Sample not collected for LAB test"));
    }
    return null;
  }
}
