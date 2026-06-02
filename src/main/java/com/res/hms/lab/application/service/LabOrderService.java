package com.res.hms.lab.application.service;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.lab.domain.LabOrder;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.domain.LabPackage;
import com.res.hms.lab.domain.LabTest;
import com.res.hms.lab.infrastructure.repository.LabOrderRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderTestRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageRepository;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabOrderService {

    private final LabOrderRepository orderRepository;
    private final LabOrderTestRepository orderTestRepository;
    private final LabTestRepository testRepository;
    private final LabPackageRepository packageRepository;
    private final HospitalContext hospitalContext;

    @Transactional
    public LabOrder createOrder(UUID patientId, List<UUID> testIds, List<UUID> packageIds) {
        UUID hospitalId = hospitalContext.requireHospitalId();
        
        LabOrder order = LabOrder.builder()
                .hospitalId(hospitalId)
                .patientId(patientId)
                .orderNumber("ORD-" + System.currentTimeMillis())
                .status("ORDERED")
                .build();
        
        order = orderRepository.save(order);

        // 1. Process Individual Tests
        for (UUID testId : testIds) {
            orderTestRepository.save(LabOrderTest.builder()
                    .orderId(order.getId())
                    .testId(testId)
                    .status("PENDING")
                    .build());
        }

        // 2. Process Packages (Logic Expansion)
        // Note: For now, I'll assume a simplified package expansion logic
        for (UUID packageId : packageIds) {
            orderTestRepository.save(LabOrderTest.builder()
                    .orderId(order.getId())
                    .packageId(packageId)
                    .status("PENDING")
                    .build());
            
            // Here, you would typically fetch individual tests in the package 
            // and add them as sub-items or specific tasks.
        }

        return order;
    }
}
