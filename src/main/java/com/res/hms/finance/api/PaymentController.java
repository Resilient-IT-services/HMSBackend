package com.res.hms.finance.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.finance.application.dto.PaymentDTO;
import com.res.hms.finance.application.dto.RecordPaymentRequest;
import com.res.hms.finance.application.service.PaymentService;
import com.res.hms.iam.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthService authService;
    private final com.res.hms.core.tenant.HospitalContext hospitalContext;

    @PostMapping
    public ApiResponse<PaymentDTO> recordPayment(@RequestBody RecordPaymentRequest request) {
        UUID hospitalId = hospitalContext.requireHospitalId();
        UUID currentUserId = authService.getAuthenticatedPrincipal().userId();
        return ApiResponse.success(paymentService.recordPayment(hospitalId, request, currentUserId));
    }
}
