package com.res.hms.billing.service;

import com.res.hms.billing.dto.AddBillingItemRequest;
import com.res.hms.billing.dto.BillingItemResponse;
import com.res.hms.billing.dto.BillingResponse;
import com.res.hms.billing.dto.CreateOpdBillingRequest;
import com.res.hms.billing.repository.BillingRepository;
import com.res.hms.common.audit.Auditable;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Billing;
import com.res.hms.domain.BillingItem;
import com.res.hms.domain.BillingSourceType;
import com.res.hms.domain.BillingStatus;
import com.res.hms.domain.Patient;
import com.res.hms.patient.service.PatientService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingService {

  private final BillingRepository billingRepository;
  private final PatientService patientService;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  @Transactional
  @Auditable(entity = "billing", action = "CREATE")
  public BillingResponse createOpdBilling(CreateOpdBillingRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();
    Patient patient = patientService.requirePatient(request.patientId(), hospitalId);

    Billing billing = new Billing();
    billing.setHospitalId(hospitalId);
    billing.setPatient(patient);
    billing.setSourceType(BillingSourceType.OPD);
    billing.setStatus(BillingStatus.DRAFT);
    billing.setTotalAmount(BigDecimal.ZERO);
    billingRepository.save(billing);

    return toResponse(billing);
  }

  @Transactional
  @Auditable(entity = "billing", action = "ADD_ITEM")
  public BillingResponse addItem(Long billingId, AddBillingItemRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();
    Billing billing = requireBilling(billingId, hospitalId);

    BillingItem item = new BillingItem();
    item.setHospitalId(hospitalId);
    item.setBilling(billing);
    item.setItemName(request.itemName());
    item.setAmount(request.amount());
    item.setQuantity(request.quantity());
    item.setSubtotal(request.amount().multiply(BigDecimal.valueOf(request.quantity())));

    billing.getItems().add(item);
    billing.setTotalAmount(
        billing.getItems().stream()
            .map(BillingItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    billing.setStatus(BillingStatus.GENERATED);

    return toResponse(billingRepository.save(billing));
  }

  @Transactional(readOnly = true)
  public BillingResponse get(Long billingId) {
    Long hospitalId = hospitalContext.requireHospitalId();
    return toResponse(requireBilling(billingId, hospitalId));
  }

  public Billing createPharmacyBilling(Patient patient) {
    Billing billing = new Billing();
    billing.setHospitalId(patient.getHospitalId());
    billing.setPatient(patient);
    billing.setSourceType(BillingSourceType.PHARMACY);
    billing.setStatus(BillingStatus.DRAFT);
    billing.setTotalAmount(BigDecimal.ZERO);
    return billingRepository.save(billing);
  }

  public Billing appendPharmacyItem(
      Billing billing, String itemName, BigDecimal amount, int quantity) {
    BillingItem item = new BillingItem();
    item.setHospitalId(billing.getHospitalId());
    item.setBilling(billing);
    item.setItemName(itemName);
    item.setAmount(amount);
    item.setQuantity(quantity);
    item.setSubtotal(amount.multiply(BigDecimal.valueOf(quantity)));
    billing.getItems().add(item);
    billing.setTotalAmount(billing.getTotalAmount().add(item.getSubtotal()));
    billing.setStatus(BillingStatus.GENERATED);
    return billingRepository.save(billing);
  }

  private Billing requireBilling(Long billingId, Long hospitalId) {
    return resourceLookup.findRequired(
        billingId, hospitalId, billingRepository::findByIdAndHospitalId, "Billing");
  }

  private BillingResponse toResponse(Billing billing) {
    return new BillingResponse(
        billing.getId(),
        billing.getPatient().getId(),
        billing.getSourceType(),
        billing.getStatus(),
        billing.getTotalAmount(),
        billing.getItems().stream()
            .map(
                item ->
                    new BillingItemResponse(
                        item.getId(),
                        item.getItemName(),
                        item.getAmount(),
                        item.getQuantity(),
                        item.getSubtotal()))
            .toList(),
        billing.getCreatedAt());
  }
}
