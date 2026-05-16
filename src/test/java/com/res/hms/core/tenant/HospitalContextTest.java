package com.res.hms.core.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HospitalContextTest {

    @Autowired private HospitalContext hospitalContext;

    @Test
    void testHospitalContext() {
        UUID id = UUID.randomUUID();
        TenantContext.setHospitalId(id);
        assertThat(hospitalContext.getHospitalIdOrNull()).isEqualTo(id);
        TenantContext.clear();
    }
}
