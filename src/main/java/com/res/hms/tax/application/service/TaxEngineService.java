package com.res.hms.tax.application.service;

import com.res.hms.tax.application.dto.TaxCalculationRequest;
import com.res.hms.tax.application.dto.TaxCalculationResult;
import com.res.hms.tax.application.dto.TaxComponent;
import com.res.hms.tax.domain.TaxGroup;
import com.res.hms.tax.domain.TaxGroupDetail;
import com.res.hms.tax.domain.TaxMaster;
import com.res.hms.tax.domain.TaxRule;
import com.res.hms.tax.infrastructure.repository.TaxRuleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaxEngineService {

  private final TaxRuleRepository ruleRepository;

  public TaxCalculationResult calculate(TaxCalculationRequest request) {
    if (request.taxableAmount() == null
        || request.taxableAmount().compareTo(BigDecimal.ZERO) == 0) {
      return new TaxCalculationResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }

    TaxGroup group = resolveTaxGroup(request);

    if (group == null) {
      return new TaxCalculationResult(
          request.taxableAmount(), BigDecimal.ZERO, request.taxableAmount(), List.of());
    }

    // Determine if Inter-state (IGST) or Intra-state (CGST/SGST)
    boolean isInterstate = false;
    if (request.hospitalState() != null && request.billingState() != null) {
      isInterstate = !request.hospitalState().equalsIgnoreCase(request.billingState());
    }

    BigDecimal totalRate = BigDecimal.ZERO;
    List<TaxMaster> applicableMasters = new ArrayList<>();

    for (TaxGroupDetail detail : group.getDetails()) {
      TaxMaster master = detail.getTaxMaster();
      if (master.isActive() && shouldApply(master.getTaxType(), isInterstate)) {
        totalRate = totalRate.add(master.getRate());
        applicableMasters.add(master);
      }
    }

    BigDecimal effectiveTaxableAmount = request.taxableAmount();
    if (request.isTaxInclusive() && totalRate.compareTo(BigDecimal.ZERO) > 0) {
      // taxableAmount = totalAmount / (1 + (totalRate / 100))
      BigDecimal divisor =
          BigDecimal.ONE.add(totalRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
      effectiveTaxableAmount = request.taxableAmount().divide(divisor, 2, RoundingMode.HALF_UP);
    }

    List<TaxComponent> components = new ArrayList<>();
    BigDecimal totalTax = BigDecimal.ZERO;

    for (TaxMaster master : applicableMasters) {
      BigDecimal amount =
          effectiveTaxableAmount
              .multiply(master.getRate())
              .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

      components.add(
          new TaxComponent(master.getCode(), master.getName(), master.getRate(), amount));
      totalTax = totalTax.add(amount);
    }

    return new TaxCalculationResult(
        effectiveTaxableAmount, totalTax, effectiveTaxableAmount.add(totalTax), components);
  }

  private TaxGroup resolveTaxGroup(TaxCalculationRequest request) {
    List<TaxRule> rules =
        ruleRepository.findActiveRulesByItemTypeAndHsn(request.itemType(), request.hsnOrSacCode());

    if (rules.isEmpty()) {
      return null;
    }

    // Simplest logic: take the highest priority matched rule
    return rules.get(0).getTaxGroup();
  }

  private boolean shouldApply(String taxType, boolean isInterstate) {
    if (taxType == null) {
      return false;
    }
    taxType = taxType.toUpperCase();

    if (isInterstate) {
      // Inter-state: Apply IGST, CESS, and any non-state specific taxes
      return taxType.contains("IGST")
          || taxType.contains("CESS")
          || (!taxType.contains("CGST") && !taxType.contains("SGST") && !taxType.contains("VAT"));
    } else {
      // Intra-state: Apply CGST, SGST, VAT, CESS, and any non-IGST specific taxes
      return taxType.contains("CGST")
          || taxType.contains("SGST")
          || taxType.contains("VAT")
          || taxType.contains("CESS")
          || !taxType.contains("IGST");
    }
  }
}
