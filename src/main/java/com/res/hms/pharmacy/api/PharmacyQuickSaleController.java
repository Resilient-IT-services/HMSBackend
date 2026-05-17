package com.res.hms.pharmacy.api;

import com.res.hms.pharmacy.application.dto.QuickPharmacySaleRequest;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleResponse;
import com.res.hms.pharmacy.application.service.QuickPharmacySaleService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pharmacy/quick-sale")
@RequiredArgsConstructor
public class PharmacyQuickSaleController {

  private final QuickPharmacySaleService quickPharmacySaleService;

  @PostMapping
  public QuickPharmacySaleResponse createQuickSale(@RequestBody QuickPharmacySaleRequest request) {
    return quickPharmacySaleService.createQuickSale(request);
  }

  @GetMapping("/{id}")
  public QuickPharmacySaleResponse getSale(@PathVariable UUID id) {
    return quickPharmacySaleService.getSale(id);
  }

  @GetMapping("/recent-customers")
  public List<Object[]> searchRecentCustomers(@RequestParam String search) {
    return quickPharmacySaleService.searchRecentCustomers(search);
  }
}
