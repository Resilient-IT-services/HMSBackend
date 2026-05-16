package com.res.hms.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.res.hms.TestDataHelper;
import com.res.hms.finance.domain.Refund;
import com.res.hms.finance.domain.RefundStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RefundServiceTest {

    @Autowired private RefundService refundService;
    @Autowired private TestDataHelper testDataHelper;

    @Test
    void testRefundLifecycle() {
        UUID hospitalId = testDataHelper.createHospitalIfMissing("REFUND_SERVICE_TEST", "Refund Service Test Hospital");
        
        Refund refund = refundService.initiateRefund(hospitalId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "Test");
        assertThat(refund.getId()).isNotNull();
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.INITIATED);

        refundService.completeRefund(refund.getId());
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(refund.getProcessedAt()).isNotNull();
        
        refundService.completeRefund(UUID.randomUUID()); // Branch coverage
    }
}
