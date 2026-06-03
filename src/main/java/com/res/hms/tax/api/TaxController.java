package com.res.hms.tax.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.tax.application.dto.TaxCalculationRequest;
import com.res.hms.tax.application.dto.TaxCalculationResult;
import com.res.hms.tax.application.service.TaxEngineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Taxation")
@RestController
@RequestMapping("/api/v1/tax")
@RequiredArgsConstructor
public class TaxController {

  private final TaxEngineService taxEngineService;

  @PostMapping("/calculate")
  public ApiResponse<TaxCalculationResult> calculate(
      @Valid @RequestBody TaxCalculationRequest request) {
    return ApiResponse.success(taxEngineService.calculate(request));
  }
}
