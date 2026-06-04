package com.res.hms.lab.application.service;

import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.lab.application.dto.LabReportPreviewDTO;
import com.res.hms.lab.domain.LabDepartment;
import com.res.hms.lab.domain.LabOrder;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.domain.LabReferenceRange;
import com.res.hms.lab.domain.LabResult;
import com.res.hms.lab.domain.LabResultParameter;
import com.res.hms.lab.domain.LabSample;
import com.res.hms.lab.domain.LabTest;
import com.res.hms.lab.domain.LabTestParameter;
import com.res.hms.lab.infrastructure.repository.LabDepartmentRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderTestRepository;
import com.res.hms.lab.infrastructure.repository.LabReferenceRangeRepository;
import com.res.hms.lab.infrastructure.repository.LabResultParameterRepository;
import com.res.hms.lab.infrastructure.repository.LabResultRepository;
import com.res.hms.lab.infrastructure.repository.LabSampleRepository;
import com.res.hms.lab.infrastructure.repository.LabTestParameterRepository;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabReportService {

  private final LabOrderRepository orderRepository;
  private final LabOrderTestRepository orderTestRepository;
  private final LabTestRepository testRepository;
  private final LabDepartmentRepository departmentRepository;
  private final LabTestParameterRepository parameterRepository;
  private final LabResultRepository resultRepository;
  private final LabResultParameterRepository resultParameterRepository;
  private final LabReferenceRangeRepository rangeRepository;
  private final LabSampleRepository sampleRepository;
  private final PatientRepository patientRepository;
  private final HospitalRepository hospitalRepository;
  private final UserAccountRepository userRepository;

  @Transactional(readOnly = true)
  public LabReportPreviewDTO getReportPreview(UUID orderId) {
    LabOrder order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

    Patient patient =
        patientRepository
            .findById(order.getPatientId())
            .orElseThrow(() -> new RuntimeException("Patient not found"));

    Hospital hospital =
        hospitalRepository
            .findById(order.getHospitalId())
            .orElseThrow(() -> new RuntimeException("Hospital not found"));

    String doctorName = "System Generated";
    if (order.getDoctorId() != null) {
      doctorName =
          userRepository
              .findById(order.getDoctorId())
              .map(UserAccount::getFullName)
              .orElse("Unknown Doctor");
    }

    List<LabOrderTest> orderTests =
        orderTestRepository.findAll().stream()
            .filter(ot -> order.getId().equals(ot.getOrderId()))
            .collect(Collectors.toList());

    List<LabReportPreviewDTO.ReportTestGroupDTO> testGroups = new ArrayList<>();

    for (LabOrderTest ot : orderTests) {
      LabTest test = testRepository.findById(ot.getTestId()).orElse(null);
      if (test == null) {
        continue;
      }

      String deptName =
          departmentRepository
              .findById(test.getDepartmentId())
              .map(LabDepartment::getName)
              .orElse("General");

      List<LabReportPreviewDTO.ReportParameterDTO> parameterDTOs = new ArrayList<>();

      List<LabTestParameter> testParams =
          parameterRepository.findAll().stream()
              .filter(p -> test.getId().equals(p.getTestId()))
              .collect(Collectors.toList());

      // Find latest result for this order test
      LabSample latestSample =
          sampleRepository.findAll().stream()
              .filter(s -> ot.getId().equals(s.getOrderTestId()))
              .findFirst()
              .orElse(null);

      LabResult latestResult = null;
      if (latestSample != null) {
        latestResult =
            resultRepository.findAll().stream()
                .filter(r -> latestSample.getId().equals(r.getSampleId()))
                .findFirst()
                .orElse(null);
      }

      for (LabTestParameter tp : testParams) {
        String value = "--";
        Boolean abnormal = false;
        Boolean critical = false;

        if (latestResult != null) {
          final UUID resId = latestResult.getId();
          LabResultParameter rp =
              resultParameterRepository.findAll().stream()
                  .filter(
                      p -> resId.equals(p.getResultId()) && tp.getId().equals(p.getParameterId()))
                  .findFirst()
                  .orElse(null);

          if (rp != null) {
            value = rp.getResultValue();
            abnormal = rp.getAbnormal();
            critical = rp.getCritical();
          }
        }

        // Fetch reference range
        String refRange = "N/A";
        List<LabReferenceRange> ranges =
            rangeRepository.findAll().stream()
                .filter(r -> tp.getId().equals(r.getParameterId()))
                .collect(Collectors.toList());

        if (!ranges.isEmpty()) {
          LabReferenceRange r = ranges.get(0); // Simplification
          refRange = r.getMinValue() + " - " + r.getMaxValue();
        }

        parameterDTOs.add(
            LabReportPreviewDTO.ReportParameterDTO.builder()
                .parameterName(tp.getParameterName())
                .resultValue(value)
                .unit(tp.getUnit())
                .referenceRange(refRange)
                .abnormal(abnormal)
                .critical(critical)
                .build());
      }

      testGroups.add(
          LabReportPreviewDTO.ReportTestGroupDTO.builder()
              .testName(test.getTestName())
              .testCode(test.getTestCode())
              .departmentName(deptName)
              .parameters(parameterDTOs)
              .build());
    }

    return LabReportPreviewDTO.builder()
        .orderId(order.getId())
        .orderNumber(order.getOrderNumber())
        .orderedAt(order.getOrderedAt())
        .hospitalName(hospital.getName())
        .hospitalAddress(hospital.getAddress() + ", " + hospital.getCity())
        .hospitalPhone("Contact: N/A") // Add if available
        .patientName(patient.getFullName())
        .patientGender(patient.getGender() != null ? patient.getGender().name() : "N/A")
        .patientAge(calculateAge(patient.getDob()))
        .patientId(patient.getPatientCode())
        .doctorName(doctorName)
        .testGroups(testGroups)
        .build();
  }

  private String calculateAge(LocalDate dob) {
    if (dob == null) {
      return "N/A";
    }
    return String.valueOf(Period.between(dob, LocalDate.now()).getYears());
  }
}
