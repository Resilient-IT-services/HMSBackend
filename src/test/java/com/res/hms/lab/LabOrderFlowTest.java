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
import com.res.hms.finance.application.service.InvoiceService;
import com.res.hms.lab.application.dto.LabOrderCreateRequest;
import com.res.hms.lab.application.dto.LabOrderResponseDTO;
import com.res.hms.lab.application.dto.LabPackageResultSaveRequest;
import com.res.hms.lab.application.dto.LabQueueItemDTO;
import com.res.hms.lab.application.dto.LabResultSaveRequest;
import com.res.hms.lab.application.dto.WalkInLabOrderRequest;
import com.res.hms.lab.application.service.LabMapper;
import com.res.hms.lab.application.service.LabOrderCreationService;
import com.res.hms.lab.application.service.LabOrderResultService;
import com.res.hms.lab.application.service.LabOrderService;
import com.res.hms.lab.application.service.LabOrderStatusService;
import com.res.hms.lab.application.service.LabOrderValidationService;
import com.res.hms.lab.domain.LabOrder;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.domain.LabPackageTest;
import com.res.hms.lab.domain.LabSpecimenType;
import com.res.hms.lab.infrastructure.repository.LabHospitalPackageConfigRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderTestRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageTestRepository;
import com.res.hms.lab.infrastructure.repository.LabPriceRepository;
import com.res.hms.lab.infrastructure.repository.LabSampleRepository;
import com.res.hms.lab.infrastructure.repository.LabSpecimenTypeRepository;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import com.res.hms.lab.infrastructure.repository.LabTestSpecimenRepository;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class LabOrderFlowTest {

  @Mock private LabOrderRepository orderRepository;
  @Mock private LabOrderTestRepository orderTestRepository;
  @Mock private LabTestRepository testRepository;
  @Mock private LabPackageRepository packageRepository;
  @Mock private LabPackageTestRepository packageTestRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private LabSampleRepository sampleRepository;
  @Mock private HospitalContext hospitalContext;
  @Mock private LabTestSpecimenRepository testSpecimenRepository;
  @Mock private LabSpecimenTypeRepository specimenTypeRepository;
  @Mock private LabOrderStatusService statusService;
  @Mock private LabOrderResultService resultService;
  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private LabMapper mapper;
  @Mock private LabPriceRepository priceRepository;
  @Mock private LabHospitalPackageConfigRepository packageConfigRepository;
  @Mock private InvoiceService invoiceService;

  private LabOrderService labOrderService;
  private LabOrderCreationService creationService;

  private UUID hospitalId;
  private UUID patientId;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    patientId = UUID.randomUUID();

    LabOrderValidationService validationService =
        new LabOrderValidationService(packageTestRepository, testRepository, packageRepository);

    creationService =
        new LabOrderCreationService(
            orderRepository,
            orderTestRepository,
            testRepository,
            patientRepository,
            packageRepository,
            packageTestRepository,
            priceRepository,
            packageConfigRepository,
            invoiceService,
            hospitalContext,
            validationService);

    labOrderService =
        new LabOrderService(
            orderRepository,
            orderTestRepository,
            sampleRepository,
            patientRepository,
            testRepository,
            packageRepository,
            testSpecimenRepository,
            packageTestRepository,
            specimenTypeRepository,
            validationService,
            statusService,
            creationService,
            resultService,
            hospitalContext,
            jdbcTemplate,
            mapper);
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
  @DisplayName("Flow: Package ordering should explode into individual test rows")
  void testPackageExpansion() {
    // Given
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    UUID packageId = UUID.randomUUID();
    UUID componentTestId1 = UUID.randomUUID();
    UUID componentTestId2 = UUID.randomUUID();

    when(orderRepository.save(any(LabOrder.class)))
        .thenAnswer(
            i -> {
              LabOrder o = i.getArgument(0);
              o.setId(UUID.randomUUID());
              return o;
            });

    // Package has 2 component tests
    LabPackageTest pt1 =
        LabPackageTest.builder().packageId(packageId).testId(componentTestId1).build();
    LabPackageTest pt2 =
        LabPackageTest.builder().packageId(packageId).testId(componentTestId2).build();
    when(packageTestRepository.findByPackageId(packageId)).thenReturn(List.of(pt1, pt2));

    com.res.hms.lab.domain.LabTest mockTest1 = new com.res.hms.lab.domain.LabTest();
    mockTest1.setId(componentTestId1);
    mockTest1.setTestName("Hemoglobin");
    com.res.hms.lab.domain.LabTest mockTest2 = new com.res.hms.lab.domain.LabTest();
    mockTest2.setId(componentTestId2);
    mockTest2.setTestName("RBC Count");
    when(testRepository.findById(componentTestId1)).thenReturn(java.util.Optional.of(mockTest1));
    when(testRepository.findById(componentTestId2)).thenReturn(java.util.Optional.of(mockTest2));

    LabOrderCreateRequest request =
        LabOrderCreateRequest.builder()
            .patientId(patientId)
            .testIds(Collections.emptyList())
            .packageIds(Collections.singletonList(packageId))
            .build();

    // When
    labOrderService.createOrder(request);

    // Then: 2 LabOrderTest rows created — one per component test, each carrying the packageId
    verify(orderTestRepository, times(2)).save(argThat(t -> packageId.equals(t.getPackageId())));
    verify(orderTestRepository, times(1))
        .save(argThat(t -> componentTestId1.equals(t.getTestId())));
    verify(orderTestRepository, times(1))
        .save(argThat(t -> componentTestId2.equals(t.getTestId())));
  }

  @Test
  @DisplayName("Flow: verify delegation of status, result, and packaging methods")
  void testDelegatedMethods() {
    UUID id = UUID.randomUUID();

    // 1. markProcessing
    labOrderService.markProcessing(id);
    verify(statusService, times(1)).markProcessing(id);

    // 2. verifyResult
    labOrderService.verifyResult(id, "remarks", "COMPLETED");
    verify(statusService, times(1)).verifyResult(id, "remarks", "COMPLETED");

    // 3. lockOrderTest
    labOrderService.lockOrderTest(id);
    verify(statusService, times(1)).lockOrderTest(id);

    // 4. unlockOrderTest
    labOrderService.unlockOrderTest(id, "reason");
    verify(statusService, times(1)).unlockOrderTest(id, "reason");

    // 5. lockPackageTests
    labOrderService.lockPackageTests(id, id);
    verify(statusService, times(1)).lockPackageTests(id, id);

    // 6. unlockPackageTests
    labOrderService.unlockPackageTests(id, id, "reason");
    verify(statusService, times(1)).unlockPackageTests(id, id, "reason");

    // 7. saveResults
    LabResultSaveRequest saveReq = new LabResultSaveRequest();
    labOrderService.saveResults(saveReq);
    verify(resultService, times(1)).saveResults(saveReq);

    // 8. savePackageResults
    LabPackageResultSaveRequest pkgSaveReq = new LabPackageResultSaveRequest();
    labOrderService.savePackageResults(pkgSaveReq);
    verify(resultService, times(1)).savePackageResults(pkgSaveReq);

    // 9. getParametersForOrderTest
    labOrderService.getParametersForOrderTest(id);
    verify(resultService, times(1)).getParametersForOrderTest(id);

    // 10. getPackageResultBundle
    labOrderService.getPackageResultBundle(id, id);
    verify(resultService, times(1)).getPackageResultBundle(id, id);

    // 11. createWalkInOrder (stub dependencies)
    when(patientRepository.save(any()))
        .thenAnswer(
            i -> {
              com.res.hms.clinical.domain.Patient p = i.getArgument(0);
              p.setId(UUID.randomUUID());
              return p;
            });

    com.res.hms.finance.domain.Invoice mockInvoice = new com.res.hms.finance.domain.Invoice();
    mockInvoice.setId(UUID.randomUUID());
    when(invoiceService.getOrCreateInvoice(any(), any(), any(), any(), any()))
        .thenReturn(mockInvoice);

    when(orderRepository.save(any(LabOrder.class)))
        .thenAnswer(
            i -> {
              LabOrder o = i.getArgument(0);
              o.setId(UUID.randomUUID());
              return o;
            });

    WalkInLabOrderRequest walkInReq = new WalkInLabOrderRequest();
    labOrderService.createWalkInOrder(walkInReq);
    verify(orderRepository, times(1)).save(any(LabOrder.class));
  }

  @Test
  @DisplayName("Flow: getQueue returns mapped orders")
  void testGetQueue() {
    UUID orderId = UUID.randomUUID();
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);

    LabOrder order = new LabOrder();
    order.setId(orderId);
    order.setHospitalId(hospitalId);
    order.setStatus("ORDERED");
    order.setPatientId(patientId);

    when(orderRepository.findAll()).thenReturn(List.of(order));

    com.res.hms.clinical.domain.Patient patient = new com.res.hms.clinical.domain.Patient();
    patient.setFullName("John Doe");
    when(patientRepository.findById(patientId)).thenReturn(java.util.Optional.of(patient));

    when(orderTestRepository.findAll()).thenReturn(Collections.emptyList());

    LabOrderResponseDTO responseDTO = new LabOrderResponseDTO();
    when(mapper.toResponseDTO(any(), any(), any(), any(), any())).thenReturn(responseDTO);

    List<LabOrderResponseDTO> result = labOrderService.getQueue();
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  @DisplayName("Flow: collectSample creates sample and updates status")
  void testCollectSample() {
    UUID orderTestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    when(hospitalContext.requireUserId()).thenReturn(userId);

    LabOrderTest orderTest = new LabOrderTest();
    orderTest.setId(orderTestId);
    orderTest.setTestId(UUID.randomUUID());
    orderTest.setStatus("PENDING");
    when(orderTestRepository.findById(orderTestId)).thenReturn(java.util.Optional.of(orderTest));

    when(jdbcTemplate.queryForObject("SELECT nextval('lab.sample_seq')", Long.class))
        .thenReturn(123L);

    LabSpecimenType specimenType = new LabSpecimenType();
    specimenType.setId(UUID.randomUUID());
    when(specimenTypeRepository.findAll()).thenReturn(List.of(specimenType));

    String sampleNumber = labOrderService.collectSample(orderTestId);
    assertNotNull(sampleNumber);
    assertTrue(sampleNumber.contains("SMP-"));
    assertTrue(sampleNumber.contains("00123"));

    verify(sampleRepository, times(1)).save(any());
    verify(orderTestRepository, times(1)).save(orderTest);
    verify(statusService, times(1))
        .syncPrescriptionStatus(
            orderTestId, com.res.hms.clinical.domain.PrescriptionLabItemStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("Flow: collectPackageSamples, markPackageProcessing, verifyPackageResults")
  void testPackageWorkflowMethods() {
    UUID orderId = UUID.randomUUID();
    UUID packageId = UUID.randomUUID();
    UUID componentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    LabOrderTest component = new LabOrderTest();
    component.setId(componentId);
    component.setOrderId(orderId);
    component.setPackageId(packageId);
    component.setStatus("PENDING");

    when(orderTestRepository.findAll()).thenReturn(List.of(component));

    // Stub for collectSample flow called inside collectPackageSamples
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    when(hospitalContext.requireUserId()).thenReturn(userId);
    when(orderTestRepository.findById(componentId)).thenReturn(java.util.Optional.of(component));
    when(jdbcTemplate.queryForObject("SELECT nextval('lab.sample_seq')", Long.class))
        .thenReturn(123L);
    LabSpecimenType specimenType = new LabSpecimenType();
    specimenType.setId(UUID.randomUUID());
    when(specimenTypeRepository.findAll()).thenReturn(List.of(specimenType));

    labOrderService.collectPackageSamples(orderId, packageId);
    verify(orderTestRepository, times(1)).save(component);

    // markPackageProcessing
    component.setStatus("SAMPLE_COLLECTED");
    labOrderService.markPackageProcessing(orderId, packageId);
    verify(statusService, times(1)).markProcessing(componentId);

    // verifyPackageResults
    component.setStatus("RESULT_ENTERED");
    labOrderService.verifyPackageResults(orderId, packageId, "remarks", "VERIFIED");
    verify(statusService, times(1)).verifyResult(componentId, "remarks", "VERIFIED");
  }

  @Test
  @DisplayName("Flow: getLabQueue returns page response")
  void testGetLabQueue() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    org.springframework.data.domain.Page<LabQueueItemDTO> page =
        org.springframework.data.domain.Page.empty();
    when(orderTestRepository.findQueueByHospitalAndStatus(any(), any(), any())).thenReturn(page);

    var response = labOrderService.getLabQueue("PENDING", 0, 10);
    assertNotNull(response);
  }
}
