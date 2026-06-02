package com.res.hms.lab;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.lab.application.service.LabOrderService;
import com.res.hms.lab.domain.LabOrder;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.infrastructure.repository.LabOrderRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderTestRepository;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LabOrderFlowTest {

    @Mock private LabOrderRepository orderRepository;
    @Mock private LabOrderTestRepository orderTestRepository;
    @Mock private HospitalContext hospitalContext;
    @InjectMocks private LabOrderService labOrderService;

    private UUID hospitalId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        hospitalId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    }

    @Test
    @DisplayName("Flow: Creating an order should generate unique order number and status")
    void testOrderCreation() {
        // Given
        UUID testId = UUID.randomUUID();
        when(orderRepository.save(any(LabOrder.class))).thenAnswer(i -> {
            LabOrder o = i.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        // When
        LabOrder created = labOrderService.createOrder(patientId, Collections.singletonList(testId), Collections.emptyList());

        // Then
        assertNotNull(created.getOrderNumber());
        assertTrue(created.getOrderNumber().startsWith("ORD-"));
        assertEquals("ORDERED", created.getStatus());
        
        // Verify individual test record was created
        verify(orderTestRepository, times(1)).save(argThat(t -> t.getTestId().equals(testId)));
    }

    @Test
    @DisplayName("Flow: Package ordering should record package intent")
    void testPackageExpansion() {
        // Given
        UUID packageId = UUID.randomUUID();
        when(orderRepository.save(any(LabOrder.class))).thenAnswer(i -> {
            LabOrder o = i.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        // When
        labOrderService.createOrder(patientId, Collections.emptyList(), Collections.singletonList(packageId));

        // Then
        verify(orderTestRepository, times(1)).save(argThat(t -> t.getPackageId().equals(packageId)));
    }
}
