package com.res.hms.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.lab.application.dto.LabOrderCreateRequest;
import com.res.hms.lab.application.service.LabOrderService;
import com.res.hms.lab.domain.LabOrder;
import com.res.hms.lab.infrastructure.repository.LabOrderRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderTestRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageRepository;
import com.res.hms.lab.infrastructure.repository.LabResultParameterRepository;
import com.res.hms.lab.infrastructure.repository.LabResultRepository;
import com.res.hms.lab.infrastructure.repository.LabSampleRepository;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LabOrderFlowTest {

  @Mock private LabOrderRepository orderRepository;
  @Mock private LabOrderTestRepository orderTestRepository;
  @Mock private LabTestRepository testRepository;
  @Mock private LabPackageRepository packageRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private LabSampleRepository sampleRepository;
  @Mock private LabResultRepository resultRepository;
  @Mock private LabResultParameterRepository resultParameterRepository;
  @Mock private HospitalContext hospitalContext;
  @InjectMocks private LabOrderService labOrderService;

  private UUID hospitalId;
  private UUID patientId;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    patientId = UUID.randomUUID();
  }

  @Test
  @DisplayName("Flow: Creating an order should generate unique order number and status")
  void testOrderCreation() {
    // Given
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    UUID testId = UUID.randomUUID();
    when(orderRepository.save(any(LabOrder.class)))
        .thenAnswer(
            i -> {
              LabOrder o = i.getArgument(0);
              o.setId(UUID.randomUUID());
              return o;
            });

    com.res.hms.lab.domain.LabTest mockTest = new com.res.hms.lab.domain.LabTest();
    mockTest.setId(testId);
    mockTest.setTestName("Mock Test");
    when(testRepository.findById(testId)).thenReturn(java.util.Optional.of(mockTest));

    LabOrderCreateRequest request =
        LabOrderCreateRequest.builder()
            .patientId(patientId)
            .testIds(Collections.singletonList(testId))
            .packageIds(Collections.emptyList())
            .build();

    // When
    LabOrder created = labOrderService.createOrder(request);

    // Then
    assertNotNull(created.getOrderNumber());
    assertTrue(created.getOrderNumber().startsWith("ORD-"));
    assertEquals("ORDERED", created.getStatus());

    // Verify individual test record was created
    verify(orderTestRepository, times(1)).save(argThat(t -> testId.equals(t.getTestId())));
  }

  @Test
  @DisplayName("Flow: Package ordering should record package intent")
  void testPackageExpansion() {
    // Given
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    UUID packageId = UUID.randomUUID();
    when(orderRepository.save(any(LabOrder.class)))
        .thenAnswer(
            i -> {
              LabOrder o = i.getArgument(0);
              o.setId(UUID.randomUUID());
              return o;
            });

    LabOrderCreateRequest request =
        LabOrderCreateRequest.builder()
            .patientId(patientId)
            .testIds(Collections.emptyList())
            .packageIds(Collections.singletonList(packageId))
            .build();

    // When
    labOrderService.createOrder(request);

    // Then
    verify(orderTestRepository, times(1)).save(argThat(t -> packageId.equals(t.getPackageId())));
  }
}
